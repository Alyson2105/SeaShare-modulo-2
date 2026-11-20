-- V7__dispute_context_and_guards.sql : esquema del contexto de disputa (H9: CU-16, CU-17, CU-18)
-- V1 a V6 son intocables: todo el esquema nuevo de H9 vive en esta unica migracion.

-- ---------------------------------------------------------------------------
-- dispute_guarantee: propietario de la reserva
-- ---------------------------------------------------------------------------
-- El owner_id viaja en OpenDisputeCommand (CU-16 FR-006) para que disputa
-- autorice el reclamo sin leer el contexto reservas (D-03).
-- La tabla nunca tuvo filas: agregar NOT NULL es seguro (precedente V3).
ALTER TABLE dispute_guarantee ADD COLUMN owner_id UUID NOT NULL;

-- Barrido del cierre automatico: solo disputas abiertas, vencidas y sin reclamo.
CREATE INDEX ix_dispute_due ON dispute_guarantee (window_end)
    WHERE status = 'PENDING' AND has_claim = FALSE;

-- ---------------------------------------------------------------------------
-- D-19: los estados finales son inmutables (trigger, no solo convencion)
-- ---------------------------------------------------------------------------
CREATE FUNCTION prevent_final_dispute_mutation() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' OR OLD.status IN ('ACCEPTED', 'REJECTED') THEN
        RAISE EXCEPTION 'dispute_guarantee es inmutable en estado final';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_dispute_final_immutable BEFORE UPDATE OR DELETE ON dispute_guarantee
    FOR EACH ROW EXECUTE FUNCTION prevent_final_dispute_mutation();

-- ---------------------------------------------------------------------------
-- D-08: un reclamo por disputa y sin edicion (solo INSERT)
-- ---------------------------------------------------------------------------
CREATE TRIGGER trg_dispute_claim_immutable BEFORE UPDATE OR DELETE ON dispute_claim
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();

ALTER TABLE dispute_claim ADD CONSTRAINT ck_dispute_claim_category
    CHECK (category IS NULL OR category IN
        ('structure', 'engine', 'equipment', 'cleaning', 'delay', 'other'));

-- ---------------------------------------------------------------------------
-- admin_review: decision valida, indice de bandeja e inmutabilidad
-- ---------------------------------------------------------------------------
CREATE TRIGGER trg_admin_review_immutable BEFORE UPDATE OR DELETE ON admin_review
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();

ALTER TABLE admin_review ADD CONSTRAINT ck_admin_review_decision
    CHECK (decision IN ('PENDING', 'REJECTED', 'ACCEPTED'));
CREATE INDEX ix_admin_review_dispute ON admin_review (dispute_id, reviewed_at DESC);

-- ---------------------------------------------------------------------------
-- dispute_status_audit: historial de cambios de estado
-- (CU-16 Key Entities "historial de cambios de estado"; CU-17 FR-008/FR-009)
-- ---------------------------------------------------------------------------
CREATE TABLE dispute_status_audit (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id  UUID        NOT NULL REFERENCES dispute_guarantee (id),
    from_status VARCHAR(20),                          -- Nulo en la creacion
    to_status   VARCHAR(20) NOT NULL,
    actor_type  VARCHAR(20) NOT NULL,                 -- SYSTEM o ADMIN
    actor_id    UUID,                                 -- Nulo cuando el actor es el sistema
    reason      TEXT,                                 -- Informativo, nunca se publica a M3
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_dispute_audit_from  CHECK (from_status IS NULL OR from_status IN
        ('PENDING', 'REJECTED', 'ACCEPTED')),
    CONSTRAINT ck_dispute_audit_to    CHECK (to_status IN ('PENDING', 'REJECTED', 'ACCEPTED')),
    CONSTRAINT ck_dispute_audit_actor CHECK (actor_type IN ('SYSTEM', 'ADMIN'))
);

CREATE INDEX ix_dispute_audit ON dispute_status_audit (dispute_id, occurred_at);
CREATE TRIGGER trg_dispute_audit_immutable BEFORE UPDATE OR DELETE ON dispute_status_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
