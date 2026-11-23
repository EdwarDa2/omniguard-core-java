package com.omniguard.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "core-api", port = "8080")
class AuthControllerPactConsumerTest {

    @Pact(consumer = "omniguard-frontend")
    RequestResponsePact loginSuccess(PactDslWithProvider builder) {
        return builder
                .given("un usuario válido existe")
                .uponReceiving("una petición de login exitosa")
                .path("/api/v1/auth/login")
                .method("POST")
                .headers(Map.of(
                        "Content-Type", "application/json",
                        "Accept", "application/json"
                ))
                .body(new PactDslJsonBody()
                        .stringType("usernameOrEmail", "testuser")
                        .stringType("password", "SecurePass123!")
                        .stringType("deviceFingerprint", "device-123")
                )
                .willRespondWith()
                .status(200)
                .headers(Map.of(
                        "Content-Type", "application/json",
                        "Set-Cookie", "at=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=900"
                ))
                .body(new PactDslJsonBody()
                        .stringType("user.username", "testuser")
                        .stringType("user.email", "test@example.com")
                        .stringType("user.role.name", "ANALYST")
                        .stringType("accessToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                        .stringType("refreshToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                        .numberType("accessTokenExpiresInSeconds", 900)
                        .stringType("tokenType", "Bearer")
                )
                .toPact();
    }

    @Pact(consumer = "omniguard-frontend")
    RequestResponsePact loginInvalidCredentials(PactDslWithProvider builder) {
        return builder
                .given("un usuario no existe o credenciales inválidas")
                .uponReceiving("una petición de login con credenciales incorrectas")
                .path("/api/v1/auth/login")
                .method("POST")
                .headers(Map.of(
                        "Content-Type", "application/json",
                        "Accept", "application/json"
                ))
                .body(new PactDslJsonBody()
                        .stringType("usernameOrEmail", "nonexistent")
                        .stringType("password", "wrongpassword")
                )
                .willRespondWith()
                .status(401)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("timestamp", "2024-01-01T00:00:00Z")
                        .integerType("status", 401)
                        .stringType("error", "Unauthorized")
                        .stringType("message", "Credenciales inválidas")
                        .stringType("path", "/api/v1/auth/login")
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "loginSuccess")
    void loginSuccessTest() {
        WebClient client = WebClient.create("http://localhost:8080");
        
        Map<String, Object> response = client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "usernameOrEmail", "testuser",
                        "password", "SecurePass123!",
                        "deviceFingerprint", "device-123"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.get("user")).isNotNull();
        assertThat(response.get("accessToken")).isNotNull();
        assertThat(response.get("refreshToken")).isNotNull();
    }

    @Test
    @PactTestFor(pactMethod = "loginInvalidCredentials")
    void loginInvalidCredentialsTest() {
        WebClient client = WebClient.create("http://localhost:8080");
        
        try {
            client.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "usernameOrEmail", "nonexistent",
                            "password", "wrongpassword"
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            // Expected 401 error
            assertThat(e).isInstanceOf(org.springframework.web.reactive.function.client.WebClientResponseException.class);
        }
    }
}