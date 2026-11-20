-- V8__calculation_audit.sql : auditoria persistente de cada calculo total solicitado a M3 (CU-12 FR-010)
-- Captura la reserva, los montos recibidos, la marca temporal y el estado del calculo.
-- operation_id queda nulo mientras el contrato externo no entregue un identificador de liquidacion.

CREATE TABLE calculation_audit (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id            UUID        NOT NULL,
    operation_id              VARCHAR(100),
    rental_amount             NUMERIC(14, 4),
    insurance_amount          NUMERIC(14, 4),
    guarantee_deposit_amount  NUMERIC(14, 4),
    total_amount              NUMERIC(14, 4),
    calculation_status        VARCHAR(20) NOT NULL,
    occurred_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_calculation_audit_status CHECK (calculation_status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX ix_calculation_audit_reservation ON calculation_audit (reservation_id, occurred_at DESC);

CREATE TRIGGER trg_calculation_audit_immutable BEFORE UPDATE OR DELETE ON calculation_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
