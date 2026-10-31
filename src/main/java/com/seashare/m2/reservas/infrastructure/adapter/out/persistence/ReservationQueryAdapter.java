package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.port.out.ReservationQueryPort;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReservationQueryAdapter implements ReservationQueryPort { // adaptador de persistencia para consultas paginadas con spring data

    private final ReservationJpaRepository repository; // repositorio jpa de reservas

    public ReservationQueryAdapter(ReservationJpaRepository repository) { // inyeccion del repositorio
        this.repository = repository; // asigna repositorio
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
    public Optional<ReservationView> findById(UUID reservationId) { // consulta reserva por identificador unico
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
