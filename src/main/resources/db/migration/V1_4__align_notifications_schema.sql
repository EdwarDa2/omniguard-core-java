-- ============================================================
-- OmniGuard SOC - Migración Flyway: Alineación de Notificaciones e Integraciones
-- Agrega tablas de configuración de canales externos y reglas de severidad
-- Compatible e idempotente con 001-init.sql
-- ============================================================

CREATE SCHEMA IF NOT EXISTS settings;

CREATE TABLE IF NOT EXISTS settings.notification_configs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES iam.users(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    webhook_url TEXT,
    chat_id TEXT,
    bot_token TEXT,
    email VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settings.notification_rules (
    id BIGSERIAL PRIMARY KEY,
    notify_critical BOOLEAN NOT NULL DEFAULT true,
    notify_high BOOLEAN NOT NULL DEFAULT true,
    notify_medium BOOLEAN NOT NULL DEFAULT false,
    notify_low BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_configs_platform ON settings.notification_configs(platform);
