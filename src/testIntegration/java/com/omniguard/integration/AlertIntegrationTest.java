package com.omniguard.integration;

import com.omniguard.adapter.in.rest.dto.alert.AlertFilterRequest;
import com.omniguard.adapter.in.rest.dto.alert.AlertResponse;
import com.omniguard.adapter.in.rest.dto.alert.AlertStatusUpdateRequest;
import com.omniguard.adapter.in.rest.dto.common.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AlertIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("omniguard_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("paseto.local-key", () -> "dGVzdC1rZXktdGhlLWlzLTMyLWJ5dGVzLWxvbmdlbg==");
        registry.add("paseto.private-key", () -> "test-private-key");
        registry.add("paseto.public-key", () -> "test-public-key");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/alerts";
    }

    @Test
    void listAlerts_ShouldReturn401WithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getAlertStats_ShouldReturn401WithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/stats", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateAlertStatus_ShouldReturn401WithoutAuth() {
        UUID randomId = UUID.randomUUID();
        AlertStatusUpdateRequest request = AlertStatusUpdateRequest.builder()
                .status("IN_PROGRESS")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AlertStatusUpdateRequest> request = new HttpEntity<>(request, new HttpHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/" + UUID.randomUUID() + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(AlertStatusUpdateRequest.builder().status("IN_PROGRESS").build()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}