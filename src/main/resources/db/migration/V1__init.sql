-- V1__init.sql : esquema base del Modulo 2 (reservas, auditoria y outbox)
-- Convenciones: UUID como clave, timestamptz para fechas con zona, montos NUMERIC(18,4)
-- Regla "Sin dinero": los montos se guardan tal cual los entrega el Modulo 3, nunca se operan

-- Necesaria para la restriccion EXCLUDE de abajo (debe crearse ANTES que la tabla)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------------
-- reservation: aggregate root del modulo
-- ---------------------------------------------------------------------------
CREATE TABLE reservation (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_code         VARCHAR(30)  NOT NULL UNIQUE,    -- codigo visible para el usuario
    renter_id                UUID         NOT NULL,           -- Arrendatario
    vessel_id                UUID         NOT NULL,           -- Embarcacion (viene del Modulo 1)
    owner_id                 UUID         NOT NULL,           -- Propietario (viene del Modulo 1)
    status                   VARCHAR(20)  NOT NULL,           -- Ver CHECK de abajo
    cancellation_subtype     VARCHAR(20),                     -- Solo si status = CANCELLED
    start_date               DATE         NOT NULL,           -- Dia completo, en zona horaria del puerto
    end_date                 DATE         NOT NULL,           -- El ultimo dia cuenta como ocupado
    departure_time           TIME,                            -- Hora pactada de salida
    passengers               INTEGER      NOT NULL,
    holder_name              VARCHAR(150) NOT NULL,           -- Datos del titular
    holder_phone             VARCHAR(30),
    holder_email             VARCHAR(150),
    estimated_total          NUMERIC(18,4),                   -- Montos: solo almacenados, jamas operados
    rental_amount            NUMERIC(18,4),
    insurance_amount         NUMERIC(18,4),
    guarantee_deposit_amount NUMERIC(18,4),
    total_amount             NUMERIC(18,4),                   -- Nulo hasta PENDING_PAYMENT
    currency                 CHAR(3)      NOT NULL DEFAULT 'COP',
    ttl_expires_at           TIMESTAMPTZ,                     -- Vencimiento (15 min); lo barre TtlExpiryJob
    actual_departure_at      TIMESTAMPTZ,                     -- Check-in real
    actual_arrival_at        TIMESTAMPTZ,                     -- Check-out real
    completion_notes         TEXT,
    payment_reference        VARCHAR(100),
    payment_confirmed_at     TIMESTAMPTZ,
    m1_blocked               BOOLEAN      NOT NULL DEFAULT FALSE,  -- Ya se notifico RESERVED al Modulo 1
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                  BIGINT       NOT NULL DEFAULT 0,      -- Optimistic locking (@Version)

    CONSTRAINT ck_reservation_dates      CHECK (end_date >= start_date),
    CONSTRAINT ck_reservation_passengers CHECK (passengers > 0),
    CONSTRAINT ck_reservation_status     CHECK (status IN (
        'INITIATED', 'PENDING_PAYMENT', 'RESERVED', 'IN_NAVIGATION',
        'COMPLETED', 'CANCELLED', 'EXPIRED', 'PAYMENT_FAILED')),
    CONSTRAINT ck_reservation_subtype    CHECK (cancellation_subtype IS NULL OR cancellation_subtype IN (
        'FLEXIBLE', 'MODERATE', 'LATE', 'BY_OWNER', 'NO_SHOW')),

    -- Cero sobreventa: una embarcacion no puede tener dos reservas activas que se solapen.
    -- INITIATED queda fuera a proposito: pueden coexistir varias (gana la primera en pagar).
    CONSTRAINT ex_reservation_no_overlap EXCLUDE USING gist (
        vessel_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    ) WHERE (status IN ('PENDING_PAYMENT', 'RESERVED', 'IN_NAVIGATION'))
);

CREATE INDEX ix_reservation_renter ON reservation (renter_id, created_at DESC);  -- "Mis reservas" del Arrendatario
CREATE INDEX ix_reservation_owner  ON reservation (owner_id, created_at DESC);   -- Vista del Propietario
CREATE INDEX ix_reservation_ttl    ON reservation (status, ttl_expires_at);      -- Barrido de expiracion
CREATE INDEX ix_reservation_vessel ON reservation (vessel_id, start_date, end_date);

-- ---------------------------------------------------------------------------
-- reservation_status_audit: historial inmutable de cambios de estado
-- ---------------------------------------------------------------------------
CREATE TABLE reservation_status_audit (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id       UUID         NOT NULL REFERENCES reservation (id),
    from_status          VARCHAR(20),                         -- Nulo en la creacion
    to_status            VARCHAR(20)  NOT NULL,
    cancellation_subtype VARCHAR(20),
    actor                VARCHAR(50)  NOT NULL,               -- Quien disparo el cambio (usuario o sistema)
    reason               TEXT,
    occurred_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_status_audit_reservation ON reservation_status_audit (reservation_id, occurred_at);

-- Impide modificar o borrar el historial (solo se permite INSERT)
CREATE FUNCTION prevent_audit_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'reservation_status_audit es inmutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_status_audit_immutable
    BEFORE UPDATE OR DELETE ON reservation_status_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();

-- ---------------------------------------------------------------------------
-- outbox_message: eventos pendientes de publicar (patron outbox)
-- ---------------------------------------------------------------------------
CREATE TABLE outbox_message (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID         NOT NULL UNIQUE,             -- Permite al receptor deduplicar
    destination     VARCHAR(20)  NOT NULL,                    -- AMQP o M1_HTTP
    exchange        VARCHAR(100),
    routing_key     VARCHAR(150),
    payload         TEXT         NOT NULL,                    -- Cuerpo del evento (JSON)
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,                              -- Nulo = aun sin confirmar por el broker
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,

    CONSTRAINT ck_outbox_destination CHECK (destination IN ('AMQP', 'M1_HTTP'))
);

-- Indice parcial: el relay solo mira los mensajes pendientes
CREATE INDEX ix_outbox_pending ON outbox_message (next_attempt_at) WHERE published_at IS NULL;