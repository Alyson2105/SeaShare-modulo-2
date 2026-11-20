-- Versiones anexadas: el reclamo original conserva su identificador y su inmutabilidad.
CREATE TABLE dispute_claim_revision (
    revision_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES dispute_claim(id),
    owner_id UUID NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50),
    evidence_urls TEXT NOT NULL,
    edited_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_dispute_claim_revision_latest ON dispute_claim_revision (claim_id, revision_id DESC);
CREATE TRIGGER trg_dispute_claim_revision_immutable BEFORE UPDATE OR DELETE ON dispute_claim_revision
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
-- Reversion requiere preservar/exportar las revisiones; versiones antiguas solo leen el original.
