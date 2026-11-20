-- Un rechazo solo autoriza un reintento. La marca se escribe en la misma TX que el outbox.
-- El orden de insercion desempata consultas con la misma marca temporal.
ALTER TABLE payment_attempt ADD COLUMN insertion_order BIGINT GENERATED ALWAYS AS IDENTITY;
CREATE TABLE payment_retry_claim (
    payment_attempt_id UUID PRIMARY KEY REFERENCES payment_attempt(id)
);
-- Reversion: eliminar payment_retry_claim y luego la columna insertion_order; se pierde la guarda de reintentos.
-- Las filas anteriores no permiten inferir si un rechazo ya origino otro cobro:
-- reconciliar reservas PENDING_PAYMENT antes de aplicar sobre datos existentes.
-- Una instancia antigua ignora esta guarda; no ejecutar versiones mezcladas durante el cambio.
