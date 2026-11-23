-- ============================================================
-- OmniGuard SOC - Migración Flyway: Alineación y Extensión de IAM
-- Extiende iam.users y agrega iam.roles y iam.paseto_sessions
-- Compatible e idempotente con 001-init.sql de omniguard-infra
-- ============================================================

CREATE SCHEMA IF NOT EXISTS iam;

-- 1. Tabla de Roles del Sistema
CREATE TABLE IF NOT EXISTS iam.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Columnas complementarias en iam.users requeridas por Spring Security y Dominio
ALTER TABLE iam.users
    ADD COLUMN IF NOT EXISTS email VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS role_id BIGINT REFERENCES iam.roles(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64),
    ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

-- Índices para optimizar búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_users_email ON iam.users(email);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON iam.users(role_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON iam.users(username);

-- 3. Tabla de Sesiones PASETO para control y revocación de Refresh Tokens
CREATE TABLE IF NOT EXISTS iam.paseto_sessions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(100),
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX IF NOT EXISTS idx_paseto_sessions_user_id ON iam.paseto_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_paseto_sessions_expires_at ON iam.paseto_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_paseto_sessions_revoked ON iam.paseto_sessions(revoked_at);
