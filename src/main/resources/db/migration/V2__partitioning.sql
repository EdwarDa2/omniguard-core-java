-- ============================================================
-- OmniGuard SOC - Particionado de Tablas Grandes (Flyway V2)
-- Requiere pg_partman (disponible en PostgreSQL 16)
-- ============================================================

-- ------------------------------------------------------------
-- Habilitar pg_partman
-- ------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_partman;

-- ------------------------------------------------------------
-- PARTICIONADO: alerts (por RANGE en created_at, mensual)
-- Retención: 13 meses (1 año + 1 mes)
-- ------------------------------------------------------------

-- Convertir tabla alerts a particionada nativa
SELECT partman.create_parent(
    p_parent_table => 'public.alerts',
    p_control => 'created_at',
    p_type => 'range',
    p_interval => '1 month',
    p_premake => 12,
    p_automatic_maintenance => 'on',
    p_inherit_privileges => true,
    p_jobmon => false
);

-- Configurar retención
UPDATE partman.part_config
SET retention = '13 months',
    retention_keep_table = false,
    infinite_time_partitions = true
WHERE parent_table = 'public.alerts';

-- ------------------------------------------------------------
-- PARTICIONADO: audit_logs (por RANGE en changed_at, mensual)
-- Retención: 84 meses (7 años - compliance)
-- ------------------------------------------------------------

SELECT partman.create_parent(
    p_parent_table => 'public.audit_logs',
    p_control => 'changed_at',
    p_type => 'range',
    p_interval => '1 month',
    p_premake => 12,
    p_automatic_maintenance => 'on',
    p_inherit_privileges => true,
    p_jobmon => false
);

UPDATE partman.part_config
SET retention = '84 months',
    retention_keep_table = false,
    infinite_time_partitions = true
WHERE parent_table = 'public.audit_logs';

-- ------------------------------------------------------------
-- ÍNDICES BRIN para tablas append-only grandes
-- (Más eficientes que B-tree para datos temporales)
-- ------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_alerts_created_at_brin ON alerts USING BRIN (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_changed_at_brin ON audit_logs USING BRIN (changed_at);
CREATE INDEX IF NOT EXISTS idx_vulnerabilities_discovered_at_brin ON vulnerabilities USING BRIN (discovered_at);

-- ------------------------------------------------------------
-- JOB DE MANTENIMIENTO pg_partman
-- (Ejecutar via pg_cron o cron externo)
-- ------------------------------------------------------------

-- SELECT cron.schedule('partman-maintenance', '0 * * * *', 'SELECT partman.run_maintenance();');
-- SELECT cron.schedule('partman-cleanup', '0 2 * * *', 'SELECT partman.run_maintenance(p_analyze := true);');

-- ------------------------------------------------------------
-- VERIFICACIÓN
-- ------------------------------------------------------------
-- Ver configuración: SELECT * FROM partman.part_config;
-- Ver particiones: SELECT * FROM partman.show_partitions('public.alerts');
-- Ver estado: SELECT * FROM partman.check_default();