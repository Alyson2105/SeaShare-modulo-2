package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.dto.FleetMaintenanceWindow;
import com.seashare.m2.reservas.application.dto.MaintenanceEventCommand;
import com.seashare.m2.reservas.application.port.in.ApplyMaintenanceEventUseCase;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.shared.domain.ClockPort;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Job de respaldo de C5. Recorre las embarcaciones con reservas RESERVED proximas, consulta C3
 * y, si M1 reporta mantenimiento con fechas, sintetiza el aviso y reutiliza el mismo servicio de
 * aplicacion. Apagado por defecto: el mecanismo primario es el outbox de M1.
 */
@Component
@ConditionalOnProperty(prefix = "m2.maintenance.scan", name = "enabled", havingValue = "true")
public class MaintenanceReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceReconciliationJob.class); // trazas del job

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Bogota"); // zona por defecto del contrato

    private static final String EVENT_PREFIX = "maintenance-reconciliation:"; // prefijo del event_id determinista

    private final ReservationRepositoryPort repository; // embarcaciones con RESERVED

    private final FleetOperationalStatusPort fleet; // C3

    private final ApplyMaintenanceEventUseCase applyMaintenanceEvent; // reutiliza C5

    private final ClockPort clockPort; // reloj unico

    private final int windowDays; // ventana de busqueda en dias

    private final int batchSize; // maximo de embarcaciones por ciclo

    public MaintenanceReconciliationJob(ReservationRepositoryPort repository,
                                        FleetOperationalStatusPort fleet,
                                        ApplyMaintenanceEventUseCase applyMaintenanceEvent,
                                        ClockPort clockPort,
                                        @Value("${m2.maintenance.scan.window-days:60}") int windowDays,
                                        @Value("${m2.maintenance.scan.batch-size:100}") int batchSize) {
        this.repository = repository;
        this.fleet = fleet;
        this.applyMaintenanceEvent = applyMaintenanceEvent;
        this.clockPort = clockPort;
        this.windowDays = windowDays;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${m2.maintenance.scan.fixed-delay-ms:300000}") // 5 min por defecto
    @SchedulerLock(name = "maintenanceReconciliationJob", lockAtMostFor = "PT10M", lockAtLeastFor = "PT5S")
    public void run() {
        Instant now = clockPort.now(); // instante unico del ciclo
        LocalDate from = now.atZone(DEFAULT_ZONE).toLocalDate(); // inicio de la ventana
        LocalDate to = from.plusDays(windowDays); // fin de la ventana
        List<UUID> vessels = repository.findVesselsWithReservedBetween(from, to); // candidatos con RESERVED
        if (vessels.size() > batchSize) { // acota el ciclo
            vessels = vessels.subList(0, batchSize);
        }

        int synthesized = 0; // avisos sintetizados
        for (UUID vesselId : vessels) {
            try {
                if (fleet.isAvailable(vesselId, from, to, DEFAULT_ZONE)) { // sin mantenimiento
                    continue;
                }
                Optional<FleetMaintenanceWindow> window = fleet.maintenanceWindow(vesselId, from, to, DEFAULT_ZONE);
                if (window.isEmpty() || !window.get().until().isAfter(now)) { // sin fechas utiles
                    continue;
                }
                FleetMaintenanceWindow maintenance = window.get(); // ventana reportada por M1
                applyMaintenanceEvent.apply(new MaintenanceEventCommand(
                        eventId(vesselId, maintenance), // determinista por ventana
                        vesselId.toString(),
                        maintenance.start().toString(),
                        maintenance.until().toString(),
                        now.toString(),
                        "ADMIN")); // aviso sintetico de respaldo
                synthesized++;
            } catch (RuntimeException failure) { // un fallo no detiene el resto del ciclo
                log.warn("Maintenance reconciliation failed for vessel {}", vesselId, failure);
            }
        }
        if (synthesized > 0) {
            log.info("Maintenance reconciliation: {} aviso(s) sintetizado(s)", synthesized);
        }
    }

    static String eventId(UUID vesselId, FleetMaintenanceWindow window) { // misma ventana = mismo event_id
        return UUID.nameUUIDFromBytes((EVENT_PREFIX + vesselId + ":" + window.until())
                .getBytes(StandardCharsets.UTF_8)).toString();
    }
}
