-- ============================================================
-- OmniGuard SOC - Migración Flyway: Semilla de CVEs Oficiales NVD
-- Inserta CVEs reales de la National Vulnerability Database (NIST)
-- para servicios comunes auditados por el escáner perimetral
-- ============================================================

INSERT INTO nvd.cve_records (cve_id, description, cvss_version, cvss_score, cvss_severity, published_at, last_modified_at, nvd_json, synced_at)
VALUES
('CVE-2023-38408', 'OpenSSH PKCS#11 provider remote code execution in ssh-agent forwarding via loaded shared libraries.', '3.1', 9.8, 'CRITICAL', '2023-07-20 00:00:00+00', '2023-08-01 00:00:00+00', '{"source": "NVD", "vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}', CURRENT_TIMESTAMP),
('CVE-2021-41617', 'OpenSSH before 8.8 privilege escalation through authorized_keys helper program supplemental groups initialization.', '3.1', 7.0, 'HIGH', '2021-09-27 00:00:00+00', '2021-10-07 00:00:00+00', '{"source": "NVD", "vector": "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:H/A:H"}', CURRENT_TIMESTAMP),
('CVE-2021-23017', 'nginx 1-byte memory overwrite flaw in DNS resolver handling crafted DNS responses causing worker crash or execution.', '3.1', 9.8, 'CRITICAL', '2021-05-25 00:00:00+00', '2021-06-03 00:00:00+00', '{"source": "NVD", "vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}', CURRENT_TIMESTAMP),
('CVE-2022-41741', 'nginx memory corruption vulnerability in ngx_http_mp4_module handling truncated or malformed MP4 atom files.', '3.1', 7.8, 'HIGH', '2022-10-19 00:00:00+00', '2022-10-21 00:00:00+00', '{"source": "NVD", "vector": "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H"}', CURRENT_TIMESTAMP),
('CVE-2024-3094', 'Malicious code in upstream tarballs of xz format liblzma library affecting OpenSSH sshd daemon authentication hooks.', '3.1', 10.0, 'CRITICAL', '2024-03-29 00:00:00+00', '2024-04-02 00:00:00+00', '{"source": "NVD", "vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"}', CURRENT_TIMESTAMP),
('CVE-2019-0708', 'Remote Desktop Protocol (RDP) Remote Code Execution Vulnerability (BlueKeep) in Windows Terminal Services.', '3.0', 9.8, 'CRITICAL', '2019-05-16 00:00:00+00', '2019-06-04 00:00:00+00', '{"source": "NVD", "vector": "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}', CURRENT_TIMESTAMP)
ON CONFLICT (cve_id) DO UPDATE SET
    description = EXCLUDED.description,
    cvss_score = EXCLUDED.cvss_score,
    cvss_severity = EXCLUDED.cvss_severity;

INSERT INTO nvd.cpe_matches (cve_id, cpe_name)
SELECT v.cve_id, v.cpe_name
FROM (VALUES
    ('CVE-2023-38408', 'cpe:2.3:a:openbsd:openssh:8.9:p1:*:*:*:*:*:*'),
    ('CVE-2023-38408', 'cpe:2.3:a:openbsd:openssh:8.9:*:*:*:*:*:*'),
    ('CVE-2021-41617', 'cpe:2.3:a:openbsd:openssh:8.7:*:*:*:*:*:*'),
    ('CVE-2021-23017', 'cpe:2.3:a:f5:nginx:1.18.0:*:*:*:*:*:*'),
    ('CVE-2022-41741', 'cpe:2.3:a:f5:nginx:1.18.0:*:*:*:*:*:*'),
    ('CVE-2024-3094', 'cpe:2.3:a:tukaani:xz:5.6.0:*:*:*:*:*:*'),
    ('CVE-2019-0708', 'cpe:2.3:o:microsoft:windows:-:*:*:*:*:*:*')
) AS v(cve_id, cpe_name)
WHERE NOT EXISTS (
    SELECT 1 FROM nvd.cpe_matches m
    WHERE m.cve_id = v.cve_id AND m.cpe_name = v.cpe_name
);
