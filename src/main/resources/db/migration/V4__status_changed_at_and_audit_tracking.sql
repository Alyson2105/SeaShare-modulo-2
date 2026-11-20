-- V4__status_changed_at_and_audit_tracking.sql : campos de CU-08 pendientes en reservation y auditoria
-- Ciclo Hito H4: contiene lo necesario para T-09 (status_changed_at) y T-10 (actor_id, event_ids) en una sola migracion
-- V1, V2 y V3 son intocables: editar una migracion ya aplicada rompe el checksum de Flyway

-- ---------------------------------------------------------------------------
-- reservation: hora exacta del ultimo cambio de estado (contrato de respuesta CU-08)
-- ---------------------------------------------------------------------------
ALTER TABLE reservation ADD COLUMN status_changed_at TIMESTAMPTZ;
-- Backfill conservador: filas existentes asumen su hora de creacion como ultimo cambio conocido
UPDATE reservation SET status_changed_at = created_at WHERE status_changed_at IS NULL;
ALTER TABLE reservation ALTER COLUMN status_changed_at SET NOT NULL;

-- ---------------------------------------------------------------------------
-- reservation_status_audit: identidad del actor y eventos generados en la transaccion (CU-08 plan L204)
-- ---------------------------------------------------------------------------
ALTER TABLE reservation_status_audit ADD COLUMN actor_id UUID;    -- Nulo cuando actor = SYSTEM (sin usuario)
ALTER TABLE reservation_status_audit ADD COLUMN event_ids TEXT;   -- JSON array de UUIDs encolados en la misma transaccion
