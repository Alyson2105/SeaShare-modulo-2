package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.Reservation;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReservationRepositoryAdapter implements ReservationRepositoryPort { // adaptador de repositorio para reservas

    private final ReservationJpaRepository repository; // repositorio spring data jpa
    private final ReservationMapper mapper; // mapeador entre entidad y dominio

    public ReservationRepositoryAdapter(ReservationJpaRepository repository, ReservationMapper mapper) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
        this.mapper = mapper; // asigna mapeador
    }

    @Override
    public Optional<Reservation> findById(UUID id) { // busca reserva por identificador
        return repository.findById(id).map(mapper::toDomain); // busca entidad y mapea a dominio
    }

    @Override
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
        } catch (ObjectOptimisticLockingFailureException e) { // captura colision optimista de spring orm
            throw new VersionConflictException("Version conflict while saving reservation " + reservation.getId(), e); // traduce a excepcion de dominio
        }
    }
}
