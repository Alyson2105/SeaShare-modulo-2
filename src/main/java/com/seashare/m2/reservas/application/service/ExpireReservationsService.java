package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.ExpireReservationsUseCase;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.InvalidTransitionException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExpireReservationsService implements ExpireReservationsUseCase { // servicio que expira reservas vencidas por TTL

    private static final Logger log = LoggerFactory.getLogger(ExpireReservationsService.class); // logger del servicio de expiracion

    private static final String EXPIRY_REASON = "TTL expirado: reserva en INITIATED o PENDING_PAYMENT no completo el flujo de pago en el tiempo disponible"; // motivo estandar de expiracion por TTL

    private final ReservationRepositoryPort repository; // puerto de lectura de IDs vencidos
    private final ChangeReservationStatusUseCase changeStatus; // unico escritor de estado de reservas (CU-08, transaccional)
    private final ClockPort clockPort; // puerto de reloj; nunca se llama Instant.now() directamente
    private final int batchSize; // tamano maximo del lote por ejecucion

    public ExpireReservationsService(ReservationRepositoryPort repository,
                                     ChangeReservationStatusUseCase changeStatus,
                                     ClockPort clockPort,
                                     @Value("${m2.scheduler.ttl.batch-size:100}") int batchSize) { // inyeccion de puertos y configuracion de lote
        this.repository = repository; // asigna repositorio de lectura
        this.changeStatus = changeStatus; // asigna caso de uso de cambio de estado
        this.clockPort = clockPort; // asigna puerto de reloj
        this.batchSize = batchSize; // asigna tamano de lote
    }

    @Override
    public int execute() { // expira en lote las reservas en INITIATED o PENDING_PAYMENT cuyo TTL ya vencio
        Instant now = clockPort.now(); // captura el instante actual una sola vez por ejecucion; nunca Instant.now()
        List<UUID> expiredIds = repository.findExpiredBefore(now, batchSize); // recupera IDs vencidos respetando el limite de lote

        int processed = 0; // contador de reservas procesadas correctamente
        for (UUID id : expiredIds) { // cada reserva se procesa en su propia transaccion provista por changeStatus
            try {
                changeStatus.execute(new ChangeReservationStatusCommand(
                        id, // identificador de la reserva a expirar
                        ReservationStatus.EXPIRED, // estado destino: EXPIRED
                        null, // sin subtipo de cancelacion
                        ActorType.SYSTEM, // actor: sistema automatizado
                        EXPIRY_REASON // motivo estandar de expiracion por TTL
                ));
                processed++; // incremento por expiracion exitosa
            } catch (VersionConflictException e) { // conflicto de version: otro proceso ya modifico la reserva (caso normal)
                log.debug("TTL scheduler: reserva {} omitida por conflicto de version: {}", id, e.getMessage());
            } catch (InvalidTransitionException e) { // transicion invalida: la reserva ya no esta en INITIATED ni PENDING_PAYMENT (caso normal)
                log.debug("TTL scheduler: reserva {} omitida por transicion invalida: {}", id, e.getMessage());
            } catch (Exception e) { // cualquier otra excepcion inesperada
                log.error("TTL scheduler: error inesperado al expirar reserva {}: {}", id, e.getMessage(), e);
            }
        }

        if (processed > 0) { // solo loguea si hubo actividad
            log.info("TTL scheduler: {} reservas expiradas de un lote de {}", processed, expiredIds.size()); // resumen del lote
        }
        return processed; // retorna el total de reservas expiradas en esta ejecucion
    }
}
