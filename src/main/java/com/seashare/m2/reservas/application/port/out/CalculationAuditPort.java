package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.CalculationAuditEntry;

public interface CalculationAuditPort { // puerto de salida para la auditoria de CU-12 FR-010

    void record(CalculationAuditEntry entry); // inserta un asiento inmutable del calculo
}
