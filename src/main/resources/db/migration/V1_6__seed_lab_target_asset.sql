-- ============================================================
-- OmniGuard SOC - Migración Flyway: Semilla de Activo Autorizado
-- ============================================================

INSERT INTO assets.network_assets (id, ip_address, hostname, description, os, status, first_seen_at, last_seen_at)
VALUES (1, '192.168.1.10'::inet, 'srv-lab-target', 'Servidor de pruebas autorizado del laboratorio SOC', 'Ubuntu 22.04 LTS', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (ip_address) DO UPDATE SET
    hostname = EXCLUDED.hostname,
    description = EXCLUDED.description,
    last_seen_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('assets.network_assets', 'id'), coalesce((SELECT MAX(id) FROM assets.network_assets), 1));
