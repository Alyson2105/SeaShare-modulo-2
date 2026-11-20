-- V6__payment_attempt_idempotency.sql : idempotencia estricta de CU-13 por (external_transaction_id, result)
-- La clave se evalua por reserva; dos canales (consulta y endpoint) comparten la misma restriccion.

CREATE UNIQUE INDEX ux_payment_attempt_key
    ON payment_attempt (reservation_id, external_reference, result);
