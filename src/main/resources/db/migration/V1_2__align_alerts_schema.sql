-- ============================================================
-- OmniGuard SOC - Migración Flyway: Alineación de Alertas NIDS
-- Agrega columnas de ciclo de vida de resolución para el SOC
-- Compatible e idempotente con 001-init.sql de omniguard-infra
-- ============================================================

CREATE SCHEMA IF NOT EXISTS alerts;

-- Agregar columnas de resolución para el flujo del SOC (sin romper el INSERT de Python)
ALTER TABLE alerts.events
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN IF NOT EXISTS assigned_to BIGINT REFERENCES iam.users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS resolution_notes TEXT,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;

-- Índices para filtros del dashboard SOC
CREATE INDEX IF NOT EXISTS idx_alert_events_status ON alerts.events(status);
CREATE INDEX IF NOT EXISTS idx_alert_events_rule ON alerts.events(rule);
CREATE INDEX IF NOT EXISTS idx_alert_events_assigned_to ON alerts.events(assigned_to);
