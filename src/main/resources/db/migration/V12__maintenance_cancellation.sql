-- V12__maintenance_cancellation.sql : marca de mantenimiento activo en la reserva
-- Contrato M1-M2 (consistencia-m2-m1.md §6 y §8): una cancelacion por mantenimiento no debe
-- enviar AVAILABLE a M1. La reserva guarda el indicador mientras se procesa el aviso C5.
-- V1 a V11 son intocables: editar una migracion ya aplicada rompe el checksum de Flyway.

ALTER TABLE reservation ADD COLUMN maintenance_blocked BOOLEAN NOT NULL DEFAULT FALSE;
-- TRUE = la reserva se cancelo porque la embarcacion entro en mantenimiento.
