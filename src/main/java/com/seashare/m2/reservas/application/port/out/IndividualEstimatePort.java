package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.IndividualEstimate;

import java.time.LocalDate;
import java.util.UUID;

public interface IndividualEstimatePort { // puerto de salida para estimacion individual en modulo 3 (CU-11)

    IndividualEstimate getIndividualEstimate(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers); // estima una embarcacion
}
