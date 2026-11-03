package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.StatusAuditEntry;

public interface StatusAuditPort { // puerto para asentar auditoria inmutable

    void record(StatusAuditEntry entry); // inserta registro de auditoria
}
