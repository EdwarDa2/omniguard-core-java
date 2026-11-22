# ============================================================
# OmniGuard Core API - Dockerfile Multi-stage
# Spring Boot 3.2 + Java 21 + Gradle
# ============================================================

# ---- STAGE 1: Build ----
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Copiar archivos de configuración de Gradle primero (cache de dependencias)
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle

# Descargar dependencias (cache layer)
RUN gradle dependencies --no-daemon

# Copiar código fuente
COPY src ./src

# Compilar y testear (sin tests de integración que requieren BD)
RUN gradle build -x test -x integrationTest --no-daemon

# ---- STAGE 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL org.opencontainers.image.title="OmniGuard Core API" \
      org.opencontainers.image.description="Spring Boot 3.2 Hexagonal API for OmniGuard SOC" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="OmniGuard Team" \
      org.opencontainers.image.source="https://github.com/tu-org/omniguard-soc" \
      org.opencontainers.image.licenses="Proprietary"

# Usuario no-root
RUN addgroup -g 1000 -S omniguard && \
    adduser -u 1000 -S -G omniguard omniguard

# Directorio de trabajo
WORKDIR /app

# Copiar JAR compilado
COPY --from=builder /app/build/libs/*.jar app.jar

# Cambiar ownership
RUN chown -R omniguard:omniguard /app

# Usuario no-root
USER omniguard

# JVM Optimizada para contenedores
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Puertos
EXPOSE 8080 8081

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health/liveness || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]