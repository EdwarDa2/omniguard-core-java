-- ============================================================
-- OmniGuard SOC - Migración Flyway: Alineación de Activos y Superficie de Ataque
-- Agrega columnas para inventario del SOC (descripción, SO, banner de servicio)
-- Compatible e idempotente con 001-init.sql y 002-nvd.sql de omniguard-infra
-- ============================================================

CREATE SCHEMA IF NOT EXISTS assets;
CREATE SCHEMA IF NOT EXISTS nvd;

-- Agregar columnas descriptivas para el inventario de activos del SOC
ALTER TABLE assets.network_assets
    ADD COLUMN IF NOT EXISTS description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS os VARCHAR(100);

-- Agregar columna para almacenar el banner crudo del servicio detectado
ALTER TABLE assets.services
    ADD COLUMN IF NOT EXISTS banner TEXT;

-- Índices para búsquedas y optimización de filtros en el SOC
CREATE INDEX IF NOT EXISTS idx_network_assets_status ON assets.network_assets(status);
CREATE INDEX IF NOT EXISTS idx_network_assets_ip_address ON assets.network_assets(ip_address);
CREATE INDEX IF NOT EXISTS idx_services_asset_id ON assets.services(asset_id);
