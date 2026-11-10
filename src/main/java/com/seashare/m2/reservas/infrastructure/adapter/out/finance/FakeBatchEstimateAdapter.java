package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.BatchEstimateResult;
import com.seashare.m2.reservas.application.port.out.BatchEstimatePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 3.
@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeBatchEstimateAdapter implements BatchEstimatePort { // adaptador simulado de estimacion en lote

    @Override
    public BatchEstimateResult getBatchEstimates(List<UUID> vesselIds) { // retorna datos fijos de prueba
        Map<UUID, BigDecimal> estimates = new LinkedHashMap<>(); // mapa con el mismo orden de entrada
        for (UUID vesselId : vesselIds) { // todas las embarcaciones quedan disponibles
            estimates.put(vesselId, new BigDecimal("150000.00")); // monto fijo sin aritmetica
        }
        return new BatchEstimateResult(estimates, List.of()); // nadie queda no disponible en desarrollo
    }
}
