package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.port.in.CloseExpiredDisputesUseCase;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.shared.domain.ClockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cierre automatico de la ventana de 24 h (CU-16 FR-008, CU-17 FR-006).
 *
 * <p>Barrido durable sobre {@code window_end}: no hay reintento interno (D-7),
 * cada disputa se resuelve en su propia transaccion delegando en
 * {@link UpdateDisputeStatusUseCase} y un fallo individual no frena el lote.
 * Solo cierra disputas PENDING sin reclamo; con reclamo no se cierra nunca.</p>
 */
@Service
public class CloseExpiredDisputesService implements CloseExpiredDisputesUseCase {

    private static final Logger log = LoggerFactory.getLogger(CloseExpiredDisputesService.class); // trazas del barrido

    private final DisputeRepositoryPort repository; // lectura de disputas vencidas

    private final UpdateDisputeStatusUseCase updateDisputeStatus; // transaccion por disputa

    private final ClockPort clockPort; // reloj inyectable

    private final int batchSize; // maximo de disputas por ciclo

    public CloseExpiredDisputesService(DisputeRepositoryPort repository,
                                       UpdateDisputeStatusUseCase updateDisputeStatus,
                                       ClockPort clockPort,
                                       @Value("${m2.scheduler.dispute.batch-size:100}") int batchSize) {
        this.repository = repository;
        this.updateDisputeStatus = updateDisputeStatus;
        this.clockPort = clockPort;
        this.batchSize = batchSize;
    }

    @Override
    public int execute() {
        Instant now = clockPort.now(); // una sola lectura por ciclo
        List<UUID> due = repository.findPendingDue(now, batchSize); // status PENDING, sin reclamo y vencidas
        int processed = 0;

        for (UUID id : due) { // cada disputa en su propia transaccion
            try {
                updateDisputeStatus.update(new UpdateDisputeStatusCommand(
                        id,
                        DisputeStatus.REJECTED,
                        null,
                        GuaranteeDispute.SYSTEM_CLOSE_REASON,
                        null,
                        DisputeActorType.SYSTEM));
                processed++;
            } catch (DisputeRuleException e) { // ya resuelta o reclamada entre la lectura y la transicion
                log.debug("Cierre de disputas: disputa {} omitida: {}", id, e.getMessage());
            } catch (Exception e) { // un fallo individual no frena el lote
                log.error("Cierre de disputas: error inesperado al cerrar {}: {}", id, e.getMessage(), e);
            }
        }

        if (processed > 0) {
            log.info("Cierre de disputas: {} disputa(s) cerrada(s) de un lote de {}", processed, due.size());
        }
        return processed;
    }
}
