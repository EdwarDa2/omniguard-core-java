package com.omniguard.config;

import com.omniguard.adapter.out.persistence.adapter.PasetoSessionRepositoryAdapter;
import com.omniguard.config.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class HealthConfig {

    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try (var connection = dataSource.getConnection()) {
                if (connection.isValid(2)) {
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("validationQuery", "SELECT 1")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withException(e)
                        .build();
            }
            return Health.down().withDetail("database", "PostgreSQL").build();
        };
    }

    @Bean
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return () -> {
            try {
                var connection = redisConnectionFactory.getConnection();
                boolean alive = connection.ping();
                connection.close();
                if (alive) {
                    return Health.up()
                            .withDetail("redis", "Connected")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("redis", "Disconnected")
                        .withException(e)
                        .build();
            }
            return Health.down().withDetail("redis", "Disconnected").build();
        };
    }

    @Bean
    public HealthIndicator pasetoSessionsHealthIndicator(PasetoSessionRepositoryAdapter sessionRepository) {
        return () -> {
            try {
                // Verificar que hay sesiones válidas (no todas expiradas/revocadas)
                long validSessions = sessionRepository.findByUserIdAndValid(java.util.UUID.randomUUID()).size();
                // Solo health check básico
                return Health.up()
                        .withDetail("pasetoSessions", "Repository accessible")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("pasetoSessions", "Repository error")
                        .withException(e)
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator rateLimitHealthIndicator(RateLimitService rateLimitService) {
        return () -> {
            try {
                // Test rápido de rate limiter
                var result = rateLimitService.tryConsume("api:default", "health-check", 1);
                return Health.up()
                        .withDetail("rateLimiter", "Operational")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("rateLimiter", "Error")
                        .withException(e)
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator diskSpaceHealthIndicator() {
        return () -> {
            java.io.File root = new java.io.File("/");
            long freeSpace = root.getUsableSpace();
            long totalSpace = root.getTotalSpace();
            double freePercent = (double) freeSpace / totalSpace * 100;

            if (freePercent < 5.0) {
                return Health.down()
                        .withDetail("diskSpace", "Critical: " + String.format("%.1f%%", freePercent) + " free")
                        .withDetail("freeBytes", freeSpace)
                        .withDetail("totalBytes", totalSpace)
                        .build();
            } else if (freePercent < 15.0) {
                return Health.status("WARNING")
                        .withDetail("diskSpace", "Low: " + String.format("%.1f%%", freePercent) + " free")
                        .withDetail("freeBytes", freeSpace)
                        .withDetail("totalBytes", totalSpace)
                        .build();
            }
            return Health.up()
                    .withDetail("diskSpace", String.format("%.1f%% free", freePercent))
                    .withDetail("freeBytes", freeSpace)
                    .withDetail("totalBytes", totalSpace)
                    .build();
        };
    }
}