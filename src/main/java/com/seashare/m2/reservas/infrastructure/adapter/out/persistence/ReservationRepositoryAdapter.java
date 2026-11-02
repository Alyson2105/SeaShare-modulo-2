package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.NewReservation;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.persistence.EntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class ReservationRepositoryAdapter implements ReservationRepositoryPort { // adaptador de repositorio para reservas

    private final ReservationJpaRepository repository; // repositorio spring data jpa
    private final ReservationMapper mapper; // mapeador entre entidad y dominio
    private final EntityManager entityManager; // administrador de persistencia jpa

    public ReservationRepositoryAdapter(ReservationJpaRepository repository, ReservationMapper mapper, EntityManager entityManager) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
        this.mapper = mapper; // asigna mapeador
        this.entityManager = entityManager; // asigna entity manager
    }

    @Override
    public Optional<Reservation> findById(UUID id) { // busca reserva por identificador
        return repository.findById(id).map(mapper::toDomain); // busca entidad y mapea a dominio
    }

    @Override
    @Transactional // asegura lectura, comparacion y guardado en una sola transaccion
    public Reservation save(Reservation reservation) { // guarda cambios en la reserva controlando versiones
        ReservationEntity entity = repository.findById(reservation.getId()) // carga entidad existente por id
                .orElseThrow(() -> new ReservationNotFoundException(reservation.getId())); // lanza excepcion si no existe

        if (!Objects.equals(entity.getVersion(), reservation.getVersion())) { // verifica version de la entidad contra el dominio
            throw new VersionConflictException("Version conflict for reservation " + reservation.getId()); // lanza conflicto de version
        }

        mapper.applyChanges(reservation, entity); // aplica unicamente status y subtipo a la entidad

        try { // bloque protegido contra concurrencia de base de datos
            ReservationEntity saved = repository.saveAndFlush(entity); // guarda y fuerza flush inmediato
            return mapper.toDomain(saved); // retorna dominio con version actualizada
        } catch (OptimisticLockingFailureException e) { // captura colision optimista de base de datos
            throw new VersionConflictException("Version conflict while saving reservation " + reservation.getId(), e); // traduce a excepcion de dominio
        }
    }

    @Override
    public boolean existsOverlap(UUID vesselId, LocalDate startDate, LocalDate endDate, Set<ReservationStatus> blockingStatuses) { // consulta solapamientos
        return repository.existsOverlapping(vesselId, blockingStatuses, startDate, endDate); // delega en consulta jpql optimizada
    }

    @Override
    public ReservationView create(NewReservation reservation) { // persiste la nueva reserva con todas sus columnas obligatorias
        ReservationEntity entity = new ReservationEntity(); // crea instancia de entidad
        entity.setReservationCode(generateUniqueReservationCode()); // genera codigo unico con prefijo RS
        entity.setRenterId(reservation.renterId()); // asigna arrendatario
        entity.setVesselId(reservation.vesselId()); // asigna embarcacion
        entity.setOwnerId(reservation.ownerId()); // asigna propietario
        entity.setStatus(ReservationStatus.INITIATED); // fija estado inicial en INITIATED
        entity.setStartDate(reservation.startDate()); // asigna fecha de inicio
        entity.setEndDate(reservation.endDate()); // asigna fecha de fin
        entity.setDepartureTime(reservation.departureTime()); // asigna hora de salida
        entity.setPassengers(reservation.passengers()); // asigna cantidad de pasajeros
        entity.setHolderName(reservation.holderName()); // asigna nombre del titular
        entity.setHolderPhone(reservation.holderPhone()); // asigna telefono de contacto
        entity.setHolderEmail(reservation.holderEmail()); // asigna correo electronico
        entity.setEstimatedTotal(reservation.estimatedTotal()); // almacena monto estimado literal
        entity.setCurrency("COP"); // fija moneda en COP
        entity.setTtlExpiresAt(reservation.ttlExpiresAt()); // asigna expiracion de ttl
        entity.setM1Blocked(false); // indica que no bloquea en modulo 1
        entity.setCreatedAt(reservation.createdAt()); // asigna fecha de creacion provista por reloj
        entity.setHarborTimezone(reservation.harborTimezone()); // asigna zona horaria del puerto

        ReservationEntity saved = repository.saveAndFlush(entity); // persiste y fuerza envio inmediato a base de datos
        entityManager.refresh(saved); // refresca entidad para sincronizar escala numerica y valores generados

        return toView(saved); // retorna proyeccion de lectura
    }

    @Override
    public Optional<ReservationView> findViewById(UUID reservationId) { // recupera reserva por identificador
        return repository.findById(reservationId).map(this::toView); // busca y transforma a vista
    }

    // [NEEDS CLARIFICATION] formato de reservation_code no definido en los specs; se propone RS- + 8 hex.
    private String generateUniqueReservationCode() { // genera codigo legible unico para la reserva
        for (int i = 0; i < 5; i++) { // hasta 5 intentos de generacion
            String code = "RS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT); // formato RS mas 8 hex
            if (!repository.existsByReservationCode(code)) { // comprueba unicidad en la base de datos
                return code; // retorna codigo disponible
            }
        }
        throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Failed to generate unique reservation code after multiple attempts"); // error interno por agotamiento
    }

    @Override
    public ReservationPage findByRenter(UUID renterId, int page, int limit) { // consulta reservas de arrendatario con paginacion
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")); // convierte a indice basado en cero con orden descendente
        Page<ReservationEntity> entityPage = repository.findByRenterId(renterId, pageable); // ejecuta consulta paginada en base de datos
        List<ReservationView> items = entityPage.getContent().stream().map(this::toView).toList(); // mapea entidades a vistas
        return new ReservationPage(items, entityPage.getTotalElements()); // retorna resultado con total de filas
    }

    @Override
    public ReservationPage findByOwner(UUID ownerId, int page, int limit) { // consulta reservas de propietario con paginacion
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")); // convierte a indice basado en cero con orden descendente
        Page<ReservationEntity> entityPage = repository.findByOwnerId(ownerId, pageable); // ejecuta consulta paginada en base de datos
        List<ReservationView> items = entityPage.getContent().stream().map(this::toView).toList(); // mapea entidades a vistas
        return new ReservationPage(items, entityPage.getTotalElements()); // retorna resultado con total de filas
    }

    @Override
    public Optional<ReservationView> findByIdView(UUID reservationId) { // consulta reserva por identificador unico
        return repository.findById(reservationId).map(this::toView); // busca y proyecta a vista
    }

    @Override
    public boolean isOwnedByUser(UUID reservationId, UUID userId, String role) { // valida pertenencia de la reserva
        if ("RENTER".equalsIgnoreCase(role)) { // para arrendatario verifica renter_id
            return repository.existsByIdAndRenterId(reservationId, userId); // valida coincidencia de renter_id
        }
        if ("OWNER".equalsIgnoreCase(role)) { // para propietario verifica owner_id
            return repository.existsByIdAndOwnerId(reservationId, userId); // valida coincidencia de owner_id
        }
        return false; // cualquier otro rol no es propietario
    }

    private ReservationView toView(ReservationEntity entity) { // mapea entidad persistida a proyeccion inmutable
        return new ReservationView(
                entity.getId(), // id de la reserva
                entity.getStatus(), // estado
                entity.getVesselId(), // embarcacion
                entity.getStartDate(), // fecha inicio
                entity.getEndDate(), // fecha fin
                entity.getPassengers(), // pasajeros
                entity.getHolderName(), // nombre titular
                entity.getHolderPhone(), // telefono titular
                entity.getHolderEmail(), // correo titular
                entity.getEstimatedTotal(), // total estimado
                entity.getCurrency(), // moneda
                entity.getDepartureTime(), // hora zarpe
                entity.getHarborTimezone(), // zona horaria
                entity.getTtlExpiresAt(), // expiracion ttl
                entity.getCreatedAt() // creacion
        );
    }
}
