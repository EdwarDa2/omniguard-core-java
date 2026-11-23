package com.omniguard.integration;

import com.omniguard.adapter.in.rest.dto.auth.AuthResponse;
import com.omniguard.adapter.in.rest.dto.auth.LoginRequest;
import com.omniguard.adapter.in.rest.dto.auth.RegisterRequest;
import com.omniguard.domain.port.in.AuthUseCase;
import com.omniguard.domain.vo.Username;
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
class AuthIntegrationTest {

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
        registry.add("paseto.local-key", () -> "dGVzdC1rZXktdGhlLWlzLTMyLWJ5dGVzLWxvbmdlbg=="); // base64 de 32 bytes
        registry.add("paseto.private-key", () -> "test-private-key");
        registry.add("paseto.public-key", () -> "test-public-key");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthUseCase authUseCase;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    @Test
    void registerAndLogin_ShouldWork() {
        // 1. Registrar usuario
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("ANALYST")
                .build();

        ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getUser()).isNotNull();
        assertThat(registerResponse.getBody().getAccessToken()).isNotNull();
        assertThat(registerResponse.getBody().getRefreshToken()).isNotNull();

        // Verificar cookies
        HttpHeaders headers = registerResponse.getHeaders();
        assertThat(headers.getFirst(HttpHeaders.SET_COOKIE)).isNotNull();
        String setCookie = headers.getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("at="); // access token cookie
        assertThat(setCookie).contains("rt="); // refresh token cookie
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");

        // 2. Login con mismo usuario
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("SecurePass123!")
                .build();

        ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getUser().getUsername()).isEqualTo("testuser");
        assertThat(loginResponse.getBody().getUser().getRole().getName()).isEqualTo("ANALYST");
    }

    @Test
    void login_InvalidCredentials_ShouldReturn401() {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("wrongpassword")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_DuplicateUsername_ShouldReturn409() {
        RegisterRequest request1 = RegisterRequest.builder()
                .username("duplicateuser")
                .email("user1@example.com")
                .password("SecurePass123!")
                .role("VIEWER")
                .build();

        restTemplate.postForEntity("/api/v1/auth/register", request1, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        RegisterRequest request2 = RegisterRequest.builder()
                .username("duplicateuser")
                .email("user2@example.com")
                .password("SecurePass123!")
                .role("VIEWER")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request2, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void refreshToken_ShouldWork() {
        // Registrar y login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("refreshtest")
                .email("refresh@example.com")
                .password("SecurePass123!")
                .role("ANALYST")
                .build();

        ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        // Obtener refresh token de cookie
        String setCookie = registerResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String refreshToken = extractCookieValue(setCookie, "rt");

        // Refresh
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt=" + refreshToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.AuthResponse> refreshResponse = restTemplate.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, request, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().getAccessToken()).isNotNull();
    }

    @Test
    void logout_ShouldRevokeRefreshToken() {
        // Registrar y login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("logouttest")
                .email("logout@example.com")
                .password("SecurePass123!")
                .role("ANALYST")
                .build();

        ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        String setCookie = registerResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String refreshToken = extractCookieValue(setCookie, "rt");

        // Logout
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "rt=" + refreshToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, request, Void.class);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Intentar usar refresh token revocado
        HttpHeaders headers2 = new HttpHeaders();
        headers2.add(HttpHeaders.COOKIE, "rt=" + refreshToken);
        HttpEntity<Void> request2 = new HttpEntity<>(headers2);

        ResponseEntity<String> refreshResponse = restTemplate.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, request2, String.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_ShouldReturnCurrentUser() {
        // Registrar y login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("metest")
                .email("me@example.com")
                .password("SecurePass123!")
                .role("ANALYST")
                .build();

        ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, com.omniguard.adapter.in.rest.dto.auth.AuthResponse.class);

        String setCookie = registerResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String accessToken = extractCookieValue(setCookie, "at");

        // Llamar /me
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "at=" + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<com.omniguard.adapter.in.rest.dto.user.UserResponse> meResponse = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, request,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().getUsername()).isEqualTo("metest");
        assertThat(meResponse.getBody().getEmail()).isEqualTo("me@example.com");
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        if (setCookieHeader == null) return null;
        String[] cookies = setCookieHeader.split(";");
        for (String cookie : cookies) {
            cookie = cookie.trim();
            if (cookie.startsWith(cookieName + "=")) {
                return cookie.substring(cookieName.length() + 1);
            }
        }
        return null;
    }
}