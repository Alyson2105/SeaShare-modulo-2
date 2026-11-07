-- V2__remaining_tables.sql : eventos operativos, disputa de garantia y tablas tecnicas
-- Regla "Sin dinero": ninguna tabla de este archivo calcula ni opera montos

-- Funcion generica para tablas inmutables (solo se permite INSERT)
CREATE FUNCTION prevent_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% es inmutable', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Pagos (CU-13): intentos de pago reportados por el Modulo 3
-- ---------------------------------------------------------------------------
CREATE TABLE payment_attempt (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id     UUID         NOT NULL REFERENCES reservation (id),
    result             VARCHAR(20)  NOT NULL,        -- APPROVED / REJECTED / FAILED
    detail             TEXT,
    external_reference VARCHAR(100),                 -- Id de transaccion del Modulo 3
    consulted_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_result CHECK (result IN ('APPROVED', 'REJECTED', 'FAILED'))
);
CREATE INDEX ix_payment_attempt_reservation ON payment_attempt (reservation_id, consulted_at DESC);

-- ---------------------------------------------------------------------------
-- Eventos operativos inmutables: uno por reserva
-- ---------------------------------------------------------------------------
CREATE TABLE cancellation_event (                    -- CU-04
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id       UUID        NOT NULL UNIQUE REFERENCES reservation (id),
    actor                VARCHAR(50) NOT NULL,       -- Quien cancela (Arrendatario o Propietario)
    requested_at         TIMESTAMPTZ NOT NULL,
    anticipation_minutes BIGINT      NOT NULL,       -- Se envia al Modulo 3, que clasifica
    cancellation_subtype VARCHAR(20),
    reason               TEXT,
    justification        TEXT
);

CREATE TABLE noshow_event (                          -- CU-05
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id  UUID        NOT NULL UNIQUE REFERENCES reservation (id),
    owner_id        UUID        NOT NULL,
    reported_at     TIMESTAMPTZ NOT NULL,
    waiting_minutes INTEGER     NOT NULL,            -- Tolerancia fija de 30 min
    observations    TEXT
);

CREATE TABLE checkin_event (                         -- CU-06: inicio de navegacion
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID        NOT NULL UNIQUE REFERENCES reservation (id),
    owner_id       UUID        NOT NULL,
    actual_time    TIMESTAMPTZ NOT NULL,
    notes          TEXT
);

CREATE TABLE checkout_event (                        -- CU-07: fin de navegacion
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID        NOT NULL UNIQUE REFERENCES reservation (id),
    owner_id       UUID        NOT NULL,
    actual_time    TIMESTAMPTZ NOT NULL,
    remarks        TEXT
);

CREATE TRIGGER trg_cancellation_immutable BEFORE UPDATE OR DELETE ON cancellation_event
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER trg_noshow_immutable BEFORE UPDATE OR DELETE ON noshow_event
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER trg_checkin_immutable BEFORE UPDATE OR DELETE ON checkin_event
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER trg_checkout_immutable BEFORE UPDATE OR DELETE ON checkout_event
    FOR EACH ROW EXECUTE FUNCTION prevent_mutation();

-- ---------------------------------------------------------------------------
-- Trazabilidad de consultas al Modulo 1 (CU-09 y CU-10)
-- ---------------------------------------------------------------------------
CREATE TABLE external_query_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR(30) NOT NULL,             -- VESSEL_INFO / OPERATIONAL_STATUS
    vessel_id      UUID,
    result         VARCHAR(30) NOT NULL,             -- OK / TIMEOUT / ERROR
    correlation_id VARCHAR(64),
    consulted_at   TIMESTAMPTZ NOT NULL DEFAULT now()
    -- Sin datos tecnicos de la flota: el Modulo 1 es la unica fuente de verdad
);

-- ---------------------------------------------------------------------------
-- Disputa de garantia (CU-16, CU-17, CU-18): contexto independiente de reservas
-- ---------------------------------------------------------------------------
CREATE TABLE dispute_guarantee (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID        NOT NULL UNIQUE REFERENCES reservation (id),
    status         VARCHAR(20) NOT NULL,
    window_start   TIMESTAMPTZ NOT NULL,             -- Ventana fija de 24 h
    window_end     TIMESTAMPTZ NOT NULL,
    has_claim      BOOLEAN     NOT NULL DEFAULT FALSE,
    reason         TEXT,
    resolved_by    UUID,
    resolved_at    TIMESTAMPTZ,
    version        BIGINT      NOT NULL DEFAULT 0,   -- Optimistic locking
    CONSTRAINT ck_dispute_status CHECK (status IN ('PENDING', 'REJECTED', 'ACCEPTED'))
);
CREATE INDEX ix_dispute_window ON dispute_guarantee (status, window_end);  -- Job de cierre automatico

CREATE TABLE dispute_claim (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id    UUID        NOT NULL UNIQUE REFERENCES dispute_guarantee (id),  -- Un reclamo por disputa
    owner_id      UUID        NOT NULL,
    description   TEXT        NOT NULL,
    category      VARCHAR(50),
    evidence_urls TEXT,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE admin_review (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id     UUID        NOT NULL REFERENCES dispute_guarantee (id),
    admin_id       UUID        NOT NULL,
    decision       VARCHAR(20) NOT NULL,
    reason         TEXT,
    internal_notes TEXT,
    reviewed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Tablas tecnicas
-- ---------------------------------------------------------------------------
CREATE TABLE operational_failure (                   -- Pagos tardios, DLQ agotada, inconsistencias
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    use_case       VARCHAR(30) NOT NULL,
    reservation_id UUID,
    reason         TEXT        NOT NULL,
    payload_ref    TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved       BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE idempotency_key (                       -- Evita procesar dos veces la misma solicitud
    key          VARCHAR(150) NOT NULL,
    endpoint     VARCHAR(150) NOT NULL,
    response_ref TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (key, endpoint)
);

CREATE TABLE shedlock (                              -- Estructura exigida por ShedLock (locks de jobs)
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);