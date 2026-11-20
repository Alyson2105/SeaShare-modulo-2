package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.BatchEstimateResult;

import java.util.List;
import java.util.UUID;

public interface BatchEstimatePort { // puerto de salida para estimacion en lote en modulo 3 (CU-11)

    BatchEstimateResult getBatchEstimates(List<UUID> vesselIds); // estima varias embarcaciones en una o mas llamadas
}
