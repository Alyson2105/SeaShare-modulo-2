package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.externalquerylog;

import com.seashare.m2.reservas.application.dto.ExternalQueryLogEntry;
import com.seashare.m2.reservas.application.port.out.ExternalQueryLogPort;
import com.seashare.m2.shared.domain.ClockPort;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExternalQueryLogAdapter implements ExternalQueryLogPort { // adaptador jpa para external_query_log

    private static final int RETURNED_STATUS_MAX = 30; // limite de la columna returned_status (V5)

    private static final int CORRELATION_ID_MAX = 64; // limite de la columna correlation_id (V2)

    private final ExternalQueryLogJpaRepository repository; // repositorio jpa del registro

    private final ClockPort clockPort; // reloj inyectado para la hora de la consulta

    public ExternalQueryLogAdapter(ExternalQueryLogJpaRepository repository, ClockPort clockPort) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
        this.clockPort = clockPort; // asigna reloj
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // transaccion propia: el guardado sobrevive al rollback del llamador
    public void register(ExternalQueryLogEntry entry) { // guarda una fila del registro de consulta
        ExternalQueryLogEntity entity = new ExternalQueryLogEntity(); // instancia entidad
        entity.setType(entry.type().name()); // VESSEL_INFO u OPERATIONAL_STATUS
        entity.setVesselId(entry.vesselId()); // embarcacion consultada
        entity.setResult(entry.result().name()); // OK, TIMEOUT o ERROR
        entity.setReturnedStatus(truncate(entry.returnedStatus(), RETURNED_STATUS_MAX)); // truncado para no fallar el INSERT
        entity.setCorrelationId(truncate(MDC.get("correlationId"), CORRELATION_ID_MAX)); // puede ser nulo fuera de HTTP
        entity.setConsultedAt(clockPort.now()); // hora del reloj, nunca Instant.now()
        repository.save(entity); // escrito en su propia transaccion
    }

    private static String truncate(String value, int max) { // acorta al tamanho de la columna
        if (value == null) { // los campos nullable se quedan en null
            return null; // sin valor
        }
        return value.length() <= max ? value : value.substring(0, max); // corta sin fallar
    }
}
