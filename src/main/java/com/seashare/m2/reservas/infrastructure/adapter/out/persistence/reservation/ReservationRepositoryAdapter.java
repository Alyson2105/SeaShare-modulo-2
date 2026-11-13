package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.reservation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

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

        mapper.applyChanges(reservation, entity); // aplica estado y campos persistibles

        try { // bloque protegido contra concurrencia de base de datos
            ReservationEntity saved = repository.saveAndFlush(entity); // guarda y fuerza flush inmediato
            return mapper.toDomain(saved); // retorna dominio con version actualizada
        } catch (OptimisticLockingFailureException e) { // captura colision optimista de base de datos
            throw new VersionConflictException("Version conflict while saving reservation " + reservation.getId(), e); // traduce a excepcion de dominio
        } catch (DataIntegrityViolationException e) { // posible violacion de la exclusion GiST (23P01)
            if (isExclusionViolation(e)) { // dos reservas retuvieron las mismas fechas
                throw new ReservationRuleException(ErrorCode.CONCURRENCY_CONFLICT,
                        "La embarcacion ya no se encuentra disponible para las fechas seleccionadas"); // 409 FCFS
            }
            throw e; // otra violacion de integridad no se enmascara
        }
    }

    private boolean isExclusionViolation(Throwable error) { // busca el SQLState de exclusion de PostgreSQL
        Throwable current = error; // recorre la cadena de causas
        while (current != null) { // hasta el fondo
            if (current instanceof SQLException sql && "23P01".equals(sql.getSQLState())) { // exclusion_violation
                return true; // es la restriccion EXCLUDE de solapamiento
            }
            current = current.getCause(); // sube una causa
        }
        return false; // cualquier otro error de integridad
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(UUID id) { // CU-03: lectura con bloqueo pesimista
        return repository.findByIdForUpdate(id).map(mapper::toDomain); // fila bloqueada hasta el commit
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
    public ReservationPage findByRenter(UUID renterId, int page, int limit) { // consulta sin filtro de estado
        return findByRenter(renterId, page, limit, null); // delega en la consulta filtrada
    }

    @Override
    public ReservationPage findByOwner(UUID ownerId, int page, int limit) { // consulta sin filtro de estado
        return findByOwner(ownerId, page, limit, null); // delega en la consulta filtrada
    }

    @Override
    public ReservationPage findByRenter(UUID renterId,
                                        int page,
                                        int limit,
                                        Set<ReservationStatus> statuses) { // consulta reservas de arrendatario con paginacion y filtro
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")); // indice base cero y orden descendente
        Page<ReservationEntity> entityPage = statuses == null || statuses.isEmpty()
                ? repository.findByRenterId(renterId, pageable) // sin restriccion de estado
                : repository.findByRenterIdAndStatusIn(renterId, statuses, pageable); // restringida al filtro
        List<Reservation> items = entityPage.getContent().stream().map(mapper::toDomain).toList(); // mapea entidades a dominio
        return new ReservationPage(items, entityPage.getTotalElements()); // retorna resultado con total de filas
    }

    @Override
    public ReservationPage findByOwner(UUID ownerId,
                                       int page,
                                       int limit,
                                       Set<ReservationStatus> statuses) { // consulta reservas de propietario con paginacion y filtro
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt")); // indice base cero y orden descendente
        Page<ReservationEntity> entityPage = statuses == null || statuses.isEmpty()
                ? repository.findByOwnerId(ownerId, pageable) // sin restriccion de estado
                : repository.findByOwnerIdAndStatusIn(ownerId, statuses, pageable); // restringida al filtro
        List<Reservation> items = entityPage.getContent().stream().map(mapper::toDomain).toList(); // mapea entidades a dominio
        return new ReservationPage(items, entityPage.getTotalElements()); // retorna resultado con total de filas
    }

    @Override
    @Transactional(readOnly = true) // lectura de solo IDs, sin bloqueo de escritura
    public List<UUID> findExpiredBefore(Instant now, int batchSize) { // busca IDs vencidos en INITIATED o PENDING_PAYMENT
        Pageable limit = PageRequest.of(0, batchSize); // primer página con el tamaño de lote como límite máximo
        return repository.findExpiredBefore(now, limit); // delega en JPQL que usa el índice ix_reservation_ttl
    }

    @Override
    public long countCompletedByRenterExcluding(UUID renterId, UUID excludeReservationId) { // conteo de viajes completados en M2
        return repository.countByRenterIdAndStatusAndIdNot(renterId, ReservationStatus.COMPLETED, excludeReservationId); // excluye la reserva consultada
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPaymentCandidates(Instant since, int batchSize) { // CU-13: candidatos del job de cobro
        return repository.findPaymentCandidates(since, PageRequest.of(0, batchSize)); // pendientes y cierres recientes
    }
}

