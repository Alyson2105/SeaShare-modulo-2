package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.ExternalQueryLogEntry;

public interface ExternalQueryLogPort { // puerto de salida para registrar consultas a modulo 1 (CU-09 FR-010, CU-10 FR-009)

    void register(ExternalQueryLogEntry entry); // guarda una fila en external_query_log
}
