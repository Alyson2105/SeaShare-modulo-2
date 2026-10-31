package com.seashare.m2.reservas.application.port.out;

public interface StatusAuditPort { // puerto para asentar auditoria inmutable

    void record(StatusAuditEntry entry); // inserta registro de auditoria
}
