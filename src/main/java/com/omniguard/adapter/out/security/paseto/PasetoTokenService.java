package com.omniguard.adapter.out.security.paseto;

import com.omniguard.domain.model.User;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.security.KeyFactory;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.EdECPoint;
import java.security.spec.NamedParameterSpec;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.security.Security;

@Service
public class PasetoTokenService {

    private static final Logger log = LoggerFactory.getLogger(PasetoTokenService.class);
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // PASETO v4.local (symmetric) for access tokens - XChaCha20-Poly1305
    // PASETO v4.public (asymmetric) for refresh tokens - Ed25519
    private static final String ACCESS_TOKEN_PURPOSE = "local";
    private static final String REFRESH_TOKEN_PURPOSE = "public";

    private final PasetoSessionRepository sessionRepository;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    // Keys (en producción: cargar desde Docker Secrets / Vault)
    private byte[] localKey;
    private PrivateKey privateKey;
    private PublicKey publicKey;

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
            try {
                byte[] privateBytes = Base64.getDecoder().decode(privateKeyB64);
                byte[] publicBytes = Base64.getDecoder().decode(publicKeyB64);
                if (privateBytes.length != 32 || publicBytes.length != 32) {
                    throw new IllegalArgumentException("Las claves Ed25519 deben tener 32 bytes");
                }

                NamedParameterSpec parameterSpec = new NamedParameterSpec("Ed25519");
                KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
                EdECPrivateKeySpec privateSpec =
                        new EdECPrivateKeySpec(parameterSpec, privateBytes);

                byte[] littleEndian = publicBytes.clone();
                boolean xisOdd = (littleEndian[0] & 1) != 0;
                littleEndian[0] &= 0xFE;
                byte[] reversed = new byte[littleEndian.length];
                for (int i = 0; i < littleEndian.length; i++) {
                    reversed[i] = littleEndian[littleEndian.length - 1 - i];
                }
                EdECPoint publicPoint = new EdECPoint(xisOdd, new BigInteger(1, reversed));
                EdECPublicKeySpec publicSpec =
                        new EdECPublicKeySpec(parameterSpec, publicPoint);

                EdECPrivateKey javaPrivateKey =
                        (EdECPrivateKey) keyFactory.generatePrivate(privateSpec);
                EdECPublicKey javaPublicKey =
                        (EdECPublicKey) keyFactory.generatePublic(publicSpec);
                this.privateKey = new PrivateKey(javaPrivateKey, Version.V4);
                this.publicKey = new PublicKey(javaPublicKey, Version.V4);
            } catch (Exception exception) {
                throw new IllegalStateException("Clave PASETO Ed25519 inválida", exception);
            }
        }
    }

    @PostConstruct
    public void init() {
        if (localKey == null) {
            log.warn("PASETO_LOCAL_KEY no configurada. Generando clave simétrica de 32 bytes para desarrollo...");
            this.localKey = new byte[32];
            new java.security.SecureRandom().nextBytes(this.localKey);
        } else if (localKey.length != 32) {
            log.warn("PASETO_LOCAL_KEY tiene {} bytes. Ajustando automáticamente a exactamente 32 bytes.", localKey.length);
            this.localKey = java.util.Arrays.copyOf(localKey, 32);
        }
        if (privateKey == null || publicKey == null) {
            try {
                log.warn("PASETO_PUBLIC_KEY o PASETO_PRIVATE_KEY no configuradas en el entorno. Generando par de claves Ed25519 efímero para desarrollo...");
                java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("Ed25519");
                java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
                this.privateKey = new PrivateKey(keyPair.getPrivate(), Version.V4);
                this.publicKey = new PublicKey(keyPair.getPublic(), Version.V4);
                log.info("Par de claves Ed25519 autogenerado exitosamente para la sesión.");
            } catch (Exception e) {
                log.error("Error al generar par de claves Ed25519: {}", e.getMessage());
                throw new IllegalStateException("PASETO_PUBLIC_KEY y PASETO_PRIVATE_KEY deben configurarse", e);
            }
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
        PasetoClaims claims = parseClaims(payload);
        validateCommonClaims(claims);
        if (!"access".equals(claims.type())) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Tipo de token inválido");
        }
        return claims;
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

        String token = signPublic(payload, privateKey);

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
        String payload = verifyPublic(token, publicKey);
        PasetoClaims claims = parseClaims(payload);
        validateCommonClaims(claims);
        if (!"refresh".equals(claims.type())) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Tipo de token inválido");
        }

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
    // Crypto Helpers
    // ============================================================
    private String encryptLocal(String payload, byte[] key) {
        return Paseto.encrypt(new SecretKey(key, Version.V4), payload, "");
    }

    private String decryptLocal(String token, byte[] key) {
        if (token == null || !token.startsWith("v4.local.")) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Token inválido");
        }
        try {
            return Paseto.decrypt(new SecretKey(key, Version.V4), token, "");
        } catch (RuntimeException exception) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Token inválido");
        }
    }

    private String signPublic(String payload, PrivateKey key) {
        return Paseto.sign(key, payload, "");
    }

    private String verifyPublic(String token, PublicKey key) {
        if (!token.startsWith("v4.public.")) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Token inválido");
        }
        try {
            return Paseto.parse(key, token, "");
        } catch (SignatureException | RuntimeException exception) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Firma de token inválida");
        }
    }

    private PasetoClaims parseClaims(String payload) {
        try {
            JsonNode claims = new ObjectMapper().readTree(payload);
            String roles = claims.has("roles") ? claims.get("roles").toString() : "[]";
            PasetoClaims parsed = new PasetoClaims(
                    claims.path("sub").asText(null),
                    claims.path("jti").asText(null),
                    claims.path("exp").asText(null),
                    claims.path("type").asText(null),
                    roles
            );
            validateCommonClaims(parsed);
            return parsed;
        } catch (com.omniguard.domain.exception.AuthenticationFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Claims inválidos");
        }
    }

    private void validateCommonClaims(PasetoClaims claims) {
        if (claims.sub() == null || claims.jti() == null || claims.exp() == null) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException(
                    "Claims obligatorios ausentes"
            );
        }

        try {
            long expiration = Long.parseLong(claims.exp());
            if (Instant.now().getEpochSecond() >= expiration) {
                throw new com.omniguard.domain.exception.AuthenticationFailedException("Token expirado");
            }
        } catch (NumberFormatException exception) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Expiración inválida");
        }
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