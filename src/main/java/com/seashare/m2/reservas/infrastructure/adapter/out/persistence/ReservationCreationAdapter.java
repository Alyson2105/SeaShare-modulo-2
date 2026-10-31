package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.port.out.NewReservation;
import com.seashare.m2.reservas.application.port.out.ReservationCreationPort;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class ReservationCreationAdapter implements ReservationCreationPort { // adaptador de persistencia para creacion de reservas

    private final ReservationJpaRepository repository; // repositorio jpa de reservas
    private final EntityManager entityManager; // administrador de persistencia jpa

    public ReservationCreationAdapter(ReservationJpaRepository repository, EntityManager entityManager) { // inyeccion de dependencias
        this.repository = repository; // asigna repositorio
        this.entityManager = entityManager; // asigna entity manager
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
