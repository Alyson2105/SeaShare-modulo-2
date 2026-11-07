package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.reservation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seashare.m2.reservas.application.dto.ReservationPage;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ErrorCode;

import jakarta.persistence.EntityManager;

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
    public boolean existsOverlap(UUID vesselId, LocalDate start, LocalDate end, Set<ReservationStatus> blockingStatuses) { // consulta solapamientos
        return repository.existsOverlapping(vesselId, blockingStatuses, start, end); // delega en consulta jpql optimizada
    }

    @Override
    public Reservation create(Reservation newReservation) { // persiste la nueva reserva con todas sus columnas obligatorias
        ReservationEntity entity = mapper.toNewEntity(newReservation, generateUniqueReservationCode()); // crea instancia de entidad
        ReservationEntity saved = repository.saveAndFlush(entity); // persiste y fuerza envio inmediato a base de datos
        entityManager.refresh(saved); // refresca entidad para sincronizar escala numerica y valores generados
        return mapper.toDomain(saved); // retorna dominio
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
        List<Reservation> items = entityPage.getContent().stream().map(mapper::toDomain).toList(); // mapea entidades a dominio
        return new ReservationPage(items, entityPage.getTotalElements()); // retorna resultado con total de filas
    }

    @Override
    public ReservationPage findByOwner(UUID ownerId, int page, int limit) { // consulta reservas de propietario con paginacion
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")); // convierte a indice basado en cero con orden descendente
        Page<ReservationEntity> entityPage = repository.findByOwnerId(ownerId, pageable); // ejecuta consulta paginada en base de datos
        List<Reservation> items = entityPage.getContent().stream().map(mapper::toDomain).toList(); // mapea entidades a dominio
        return new ReservationPage(items, entityPage.getTotalElements()); // retorna resultado con total de filas
    }

    @Override
    @Transactional(readOnly = true) // lectura de solo IDs, sin bloqueo de escritura
    public List<UUID> findInitiatedExpiredBefore(Instant now, int batchSize) { // busca IDs de reservas INITIATED con TTL vencido
        Pageable limit = PageRequest.of(0, batchSize); // primer página con el tamaño de lote como límite máximo
        return repository.findIdsByStatusInitiatedAndTtlExpiredBefore(now, limit); // delega en JPQL que usa el índice ix_reservation_ttl
    }
}

