package com.omniguard.adapter.out.security.paseto;

import com.omniguard.domain.model.User;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasetoTokenService {

    private static final Logger log = LoggerFactory.getLogger(PasetoTokenService.class);

    // PASETO v4.local (symmetric) for access tokens - XChaCha20-Poly1305
    // PASETO v4.public (asymmetric) for refresh tokens - Ed25519
    private static final String ACCESS_TOKEN_PURPOSE = "local";
    private static final String REFRESH_TOKEN_PURPOSE = "public";

    private final PasetoSessionRepository sessionRepository;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    // Keys (en producción: cargar desde Docker Secrets / Vault)
    private byte[] localKey;      // 32 bytes para v4.local
    private KeyPair keyPair;      // Ed25519 para v4.public

    public PasetoTokenService(
            PasetoSessionRepository sessionRepository,
            @Value("${paseto.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            @Value("${paseto.refresh-token-ttl-days:30}") long refreshTokenTtlDays,
            @Value("${paseto.local-key:}") String localKeyB64,
            @Value("${paseto.public-key:}") String publicKeyB64,
            @Value("${paseto.private-key:}") String privateKeyB64
    ) {
        this.sessionRepository = sessionRepository;
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);

        // Cargar o generar claves
        if (localKeyB64 != null && !localKeyB64.isBlank()) {
            this.localKey = Base64.getDecoder().decode(localKeyB64);
        }
        if (publicKeyB64 != null && !publicKeyB64.isBlank() && privateKeyB64 != null && !privateKeyB64.isBlank()) {
            // Cargar key pair desde base64
            // Implementación simplificada - en producción usar KeyFactory
        }
    }

    @PostConstruct
    public void init() {
        if (localKey == null) {
            localKey = generateLocalKey();
            log.warn("PASETO local key generada automáticamente. En producción configure paseto.local-key via secret.");
        }
        if (keyPair == null) {
            keyPair = generateKeyPair();
            log.warn("PASETO key pair generada automáticamente. En producción configure paseto.public-key y paseto.private-key via secrets.");
        }
    }

    // ============================================================
    // Access Token (v4.local - symmetric, short-lived)
    // ============================================================
    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenTtl);

        // PASETO v4.local payload
        String payload = """
            {
                "iss": "omniguard-soc",
                "sub": "%s",
                "aud": "omniguard-api",
                "iat": %d,
                "exp": %d,
                "jti": "%s",
                "roles": %s,
                "type": "access"
            }
            """.formatted(
                user.id(),
                now.getEpochSecond(),
                exp.getEpochSecond(),
                UUID.randomUUID(),
                user.role() != null ? "[\"" + user.role().name() + "\"]" : "[]"
        );

        return encryptLocal(payload, localKey);
    }

    public PasetoClaims validateAccessToken(String token) {
        String payload = decryptLocal(token, localKey);
        return parseClaims(payload);
    }

    // ============================================================
    // Refresh Token (v4.public - asymmetric, long-lived, stored in DB)
    // ============================================================
    public String issueRefreshToken(User user, String deviceFingerprint, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        Instant exp = now.plus(refreshTokenTtl);
        String jti = UUID.randomUUID().toString();

        String payload = """
            {
                "iss": "omniguard-soc",
                "sub": "%s",
                "aud": "omniguard-api",
                "iat": %d,
                "exp": %d,
                "jti": "%s",
                "type": "refresh",
                "device": "%s"
            }
            """.formatted(
                user.id(),
                now.getEpochSecond(),
                exp.getEpochSecond(),
                jti,
                deviceFingerprint != null ? deviceFingerprint.replace("\"", "\\\"") : "unknown"
        );

        String token = signPublic(payload, keyPair.getPrivate());

        // Guardar hash en BD para revocación
        com.omniguard.domain.model.PasetoSession session = new com.omniguard.domain.model.PasetoSession(
                UUID.fromString(jti),
                user.id(),
                hashToken(token),
                now,
                exp,
                null,
                null,
                ipAddress != null ? com.omniguard.domain.vo.IpAddress.of(ipAddress) : null,
                userAgent
        );
        sessionRepository.save(session);

        return token;
    }

    public PasetoClaims validateRefreshToken(String token) {
        String payload = verifyPublic(token, keyPair.getPublic());
        PasetoClaims claims = parseClaims(payload);

        // Verificar en BD que no esté revocado
        UUID jti = UUID.fromString(claims.jti());
        Optional<com.omniguard.domain.model.PasetoSession> sessionOpt = sessionRepository.findById(jti);
        if (sessionOpt.isEmpty() || sessionOpt.get().revokedAt() != null) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Refresh token revocado o inválido");
        }
        if (sessionOpt.get().expiresAt().isBefore(Instant.now())) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Refresh token expirado");
        }

        return claims;
    }

    public void revokeSession(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            sessionRepository.save(new com.omniguard.domain.model.PasetoSession(
                    session.id(),
                    session.userId(),
                    session.tokenHash(),
                    session.issuedAt(),
                    session.expiresAt(),
                    Instant.now(),
                    "LOGOUT",
                    session.ipAddress(),
                    session.userAgent()
            ));
        });
    }

    public void revokeAllUserSessions(Long userId) {
        sessionRepository.findByUserId(userId).forEach(session -> {
            if (session.revokedAt() == null) {
                sessionRepository.save(new com.omniguard.domain.model.PasetoSession(
                        session.id(),
                        session.userId(),
                        session.tokenHash(),
                        session.issuedAt(),
                        session.expiresAt(),
                        Instant.now(),
                        "PASSWORD_CHANGE",
                        session.ipAddress(),
                        session.userAgent()
                ));
            }
        });
    }

    // ============================================================
    // Crypto Helpers (Implementación real usaría paseto-java o Tink)
    // ============================================================
    private byte[] generateLocalKey() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return key;
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Ed25519 no disponible", e);
        }
    }

    private String encryptLocal(String payload, byte[] key) {
        // TODO: Implementar con paseto-java v4.local (XChaCha20-Poly1305)
        // Placeholder: Base64.encode(payload) + "." + Base64.encode(key) - NO USAR EN PRODUCCIÓN
        return "v4.local." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String decryptLocal(String token, byte[] key) {
        // TODO: Implementar validación y descifrado real
        if (!token.startsWith("v4.local.")) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Token inválido");
        }
        String payload = new String(Base64.getUrlDecoder().decode(token.substring(9)), StandardCharsets.UTF_8);
        return payload;
    }

    private String signPublic(String payload, java.security.PrivateKey privateKey) {
        // TODO: Implementar con paseto-java v4.public (Ed25519)
        return "v4.public." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String verifyPublic(String token, java.security.PublicKey publicKey) {
        if (!token.startsWith("v4.public.")) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Token inválido");
        }
        return new String(Base64.getUrlDecoder().decode(token.substring(10)), StandardCharsets.UTF_8);
    }

    private PasetoClaims parseClaims(String payload) {
        // TODO: Parsear JSON real (Jackson/JSON-B)
        // Placeholder simple
        return new PasetoClaims(
                extractJson(payload, "sub"),
                extractJson(payload, "jti"),
                extractJson(payload, "exp"),
                extractJson(payload, "type"),
                extractJson(payload, "roles")
        );
    }

    private String extractJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return m.group(1);
        pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return "sha256$" + Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ============================================================
    // Scheduled: Rotación de claves (ejecutar mensualmente)
    // ============================================================
    // @Scheduled(cron = "0 0 3 1 * ?") // Primer día de mes a las 3 AM
    public void rotateKeys() {
        log.info("Rotando claves PASETO...");
        // 1. Generar nuevas claves
        // 2. Guardar en Vault/Secrets
        // 3. Revocar todos los refresh tokens antiguos (opcional)
        // 4. Las access tokens expiran solas en 15 min
    }

    public record PasetoClaims(
            String sub,
            String jti,
            String exp,
            String type,
            String roles
    ) {}
}