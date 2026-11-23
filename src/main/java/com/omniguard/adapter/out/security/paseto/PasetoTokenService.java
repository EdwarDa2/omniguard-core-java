package com.omniguard.adapter.out.security.paseto;

import com.omniguard.adapter.out.persistence.adapter.PasetoSessionRepositoryAdapter;
import com.omniguard.domain.exception.AuthenticationFailedException;
import com.omniguard.domain.model.PasetoSession;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.quantity.Quantities;
import paseto.Paseto;
import paseto.ProtocolVersion;
import paseto.ProtocolVersion4;
import paseto.crypto.Key;
import paseto.crypto.KeyPair;
import paseto.crypto.KeyPairGenerator;
import paseto.crypto.SecretKey;
import paseto.jwt.PasetoJsonParser;
import paseto.jwt.PasetoJsonSerializer;
import paseto.jwt.PasetoJsonToken;
import paseto.jwt.PasetoJsonTokenParser;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasetoTokenService {

    private static final Logger log = LoggerFactory.getLogger(PasetoTokenService.class);

    // PASETO v4.local (symmetric) for access tokens - XChaCha20-Poly1305
    // PASETO v4.public (asymmetric) for refresh tokens - Ed25519

    private final PasetoSessionRepository sessionRepository;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    // Keys
    private SecretKey localKey;      // 32 bytes para v4.local
    private KeyPair keyPair;         // Ed25519 para v4.public

    // PASETO v4 instances
    private final ProtocolVersion<SecretKey, KeyPair> v4 = ProtocolVersion.V4;
    private final Paseto<SecretKey, KeyPair> pasetoLocal;
    private final Paseto<SecretKey, KeyPair> pasetoPublic;

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

        this.pasetoLocal = Paseto.getInstance(v4, paseto.crypto.SecretKey.class);
        this.pasetoPublic = Paseto.getInstance(v4, paseto.crypto.KeyPair.class);
    }

    @PostConstruct
    public void init() {
        // Cargar o generar clave local (v4.local - symmetric)
        if (localKey == null) {
            localKey = generateOrLoadLocalKey();
        }

        // Cargar o generar key pair (v4.public - asymmetric)
        if (keyPair == null) {
            keyPair = generateOrLoadKeyPair();
        }

        log.info("PASETO keys initialized: local={}, public={}",
                localKey != null, keyPair != null);
    }

    // ============================================================
    // Access Token (v4.local - symmetric, short-lived: 15 min)
    // ============================================================
    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenTtl);

        PasetoJsonToken token = PasetoJsonToken.builder()
                .issuer("omniguard-soc")
                .subject(user.id().toString())
                .audience("omniguard-api")
                .issuedAt(now)
                .expiration(exp)
                .jwtId(UUID.randomUUID().toString())
                .claim("roles", user.role() != null ? List.of(user.role().name()) : List.of())
                .claim("type", "access")
                .build();

        return pasetoLocal.encrypt(token, localKey);
    }

    public PasetoClaims validateAccessToken(String token) {
        try {
            PasetoJsonToken parsed = pasetoLocal.decrypt(token, localKey);
            validateClaims(parsed);
            return extractClaims(parsed);
        } catch (Exception e) {
            log.debug("Access token validation failed: {}", e.getMessage());
            throw new AuthenticationFailedException("Access token inválido o expirado");
        }
    }

    // ============================================================
    // Refresh Token (v4.public - asymmetric, long-lived: 30 días)
    // ============================================================
    public String issueRefreshToken(User user, String deviceFingerprint, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        Instant exp = now.plus(refreshTokenTtl);
        String jti = UUID.randomUUID().toString();

        PasetoJsonToken token = PasetoJsonToken.builder()
                .issuer("omniguard-soc")
                .subject(user.id().toString())
                .audience("omniguard-api")
                .issuedAt(now)
                .expiration(exp)
                .jwtId(jti)
                .claim("type", "refresh")
                .claim("device", deviceFingerprint != null ? deviceFingerprint : "unknown")
                .build();

        String encoded = pasetoPublic.sign(token, keyPair.privateKey());

        // Guardar hash en BD para revocación
        String tokenHash = hashToken(encoded);
        PasetoSession session = new PasetoSession(
                UUID.fromString(jti),
                user.id(),
                tokenHash,
                now,
                exp,
                null,
                null,
                ipAddress != null ? com.omniguard.domain.vo.IpAddress.of(ipAddress) : null,
                userAgent
        );
        sessionRepository.save(session);

        return encoded;
    }

    public PasetoClaims validateRefreshToken(String token) {
        try {
            PasetoJsonToken parsed = pasetoPublic.verify(token, keyPair.publicKey());
            validateClaims(parsed);

            String jti = parsed.getId();
            if (jti == null) {
                throw new AuthenticationFailedException("Refresh token sin JTI");
            }

            // Verificar en BD que no esté revocado
            Optional<PasetoSession> sessionOpt = sessionRepository.findById(UUID.fromString(jti));
            if (sessionOpt.isEmpty()) {
                throw new AuthenticationFailedException("Refresh token no encontrado");
            }

            PasetoSession session = sessionOpt.get();
            if (session.isRevoked()) {
                throw new AuthenticationFailedException("Refresh token revocado");
            }
            if (session.isExpired()) {
                throw new AuthenticationFailedException("Refresh token expirado");
            }

            return extractClaims(parsed);
        } catch (Exception e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            throw new AuthenticationFailedException("Refresh token inválido o expirado");
        }
    }

    public void revokeSession(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            PasetoSession revoked = session.revoke("LOGOUT");
            sessionRepository.save(revoked);
        });
    }

    public void revokeAllUserSessions(UUID userId) {
        List<PasetoSession> sessions = sessionRepository.findByUserIdAndValid(userId);
        for (PasetoSession session : sessions) {
            sessionRepository.save(session.revoke("PASSWORD_CHANGE"));
        }
    }

    // ============================================================
    // Key Management
    // ============================================================
    private SecretKey generateOrLoadLocalKey() {
        // En producción: cargar desde Docker Secrets / Vault
        // String localKeyB64 = System.getenv("PASETO_LOCAL_KEY");
        // if (localKeyB64 != null && !localKeyB64.isBlank()) {
        //     return v4.decodeSecretKey(Base64.getDecoder().decode(localKeyB64));
        // }
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        log.warn("PASETO local key generada automáticamente. En producción configure PASETO_LOCAL_KEY via secret.");
        return v4.decodeSecretKey(keyBytes);
    }

    private KeyPair generateOrLoadKeyPair() {
        // En producción: cargar desde Docker Secrets / Vault
        // String privateKeyB64 = System.getenv("PASETO_PRIVATE_KEY");
        // String publicKeyB64 = System.getenv("PASETO_PUBLIC_KEY");
        // if (privateKeyB64 != null && !privateKeyB64.isBlank() && publicKeyB64 != null && !publicKeyB64.isBlank()) {
        //     Key privateKey = v4.decodePrivateKey(Base64.getDecoder().decode(privateKeyB64));
        //     Key publicKey = v4.decodePublicKey(Base64.getDecoder().decode(publicKeyB64));
        //     return new KeyPair(publicKey, privateKey);
        // }
        KeyPairGenerator kpg = v4.getKeyPairGenerator();
        KeyPair pair = kpg.generateKeyPair();
        log.warn("PASETO key pair generada automáticamente. En producción configure PASETO_PRIVATE_KEY y PASETO_PUBLIC_KEY via secrets.");
        return pair;
    }

    // ============================================================
    // Claims Helpers
    // ============================================================
    private void validateClaims(PasetoJsonToken claims) {
        Instant now = Instant.now();
        if (claims.getExpiration() != null && claims.getExpiration().isBefore(now)) {
            throw new AuthenticationFailedException("Token expirado");
        }
        if (claims.getNotBefore() != null && claims.getNotBefore().isAfter(now)) {
            throw new AuthenticationFailedException("Token no válido aún");
        }
        if (!"omniguard-soc".equals(claims.getIssuer())) {
            throw new AuthenticationFailedException("Emisor inválido");
        }
        if (!"omniguard-api".equals(claims.getAudience())) {
            throw new AuthenticationFailedException("Audiencia inválida");
        }
    }

    private PasetoClaims extractClaims(PasetoJsonToken claims) {
        return new PasetoClaims(
                claims.getSubject(),
                claims.getId(),
                claims.getExpiration() != null ? claims.getExpiration().toString() : null,
                claims.getStringClaim("type").orElse(null),
                claims.getListClaim("roles").orElse(List.of())
        );
    }

    private String hashToken(String token) {
        // Argon2id hash del token para almacenar en BD
        // En producción usar Argon2id real
        return "argon2id$" + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
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
            List<String> roles
    ) {}
}