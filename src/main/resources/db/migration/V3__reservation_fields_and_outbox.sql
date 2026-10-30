-- V3__reservation_fields_and_outbox.sql : campos nuevos de reservation, resultado de pago ampliado y control de eventos muertos
-- No se toca V1 ni V2: editar una migracion ya aplicada, incluso un comentario, rompe el checksum de Flyway

-- ---------------------------------------------------------------------------
-- reservation
-- ---------------------------------------------------------------------------
-- Zona horaria del puerto, se guarda al crear la reserva y CU-04 y CU-05 la leen de aqui (decision P3)
ALTER TABLE reservation ADD COLUMN harbor_timezone VARCHAR(50);  -- Nulo permitido, el issue no pide NOT NULL

-- La hora de salida (hora local del puerto) pasa a obligatoria (decision P1)
-- Es seguro porque la tabla esta vacia; con filas sin hora, esta linea fallaria
ALTER TABLE reservation ALTER COLUMN departure_time SET NOT NULL;

-- ---------------------------------------------------------------------------
-- payment_attempt: resultado ampliado a los 6 valores que reporta el Modulo 3
-- ---------------------------------------------------------------------------
-- Un CHECK no se puede modificar: se borra el de V2 (ck_payment_result) y se crea de nuevo con los 6 valores
-- El valor FAILED de V2 deja de ser valido; no hay filas con ese valor
ALTER TABLE payment_attempt DROP CONSTRAINT ck_payment_result;
ALTER TABLE payment_attempt ADD CONSTRAINT ck_payment_result CHECK (result IN (
    'IN_PROCESS', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'UNKNOWN'));

-- ---------------------------------------------------------------------------
-- outbox_message: evento muerto y ultimo error (reintentos agotados, CU-14 FR-005)
-- ---------------------------------------------------------------------------
ALTER TABLE outbox_message ADD COLUMN dead BOOLEAN NOT NULL DEFAULT FALSE;  -- TRUE = agoto los 5 intentos, el relay ya no lo reintenta
ALTER TABLE outbox_message ADD COLUMN last_error TEXT;                      -- Texto del ultimo fallo, nulo mientras no haya fallado
