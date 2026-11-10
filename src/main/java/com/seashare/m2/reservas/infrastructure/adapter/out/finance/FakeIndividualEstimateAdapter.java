package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.IndividualEstimate;
import com.seashare.m2.reservas.application.port.out.IndividualEstimatePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 3.
@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeIndividualEstimateAdapter implements IndividualEstimatePort { // adaptador simulado de estimacion individual

    @Override
    public IndividualEstimate getIndividualEstimate(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers) { // retorna datos fijos de prueba
        return new IndividualEstimate(new BigDecimal("150000.00"), "fake estimate warning"); // monto y warning fijos
    }
}
