package com.omniguard.adapter.in.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omniguard.config.RateLimitProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private static final String LOGIN = "/api/v1/auth/login";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private RateLimitFilter filter(boolean enabled) {
        RateLimitProperties props = new RateLimitProperties(
                enabled,
                true,
                List.of(
                        new RateLimitProperties.Rule(LOGIN, 5, 1),
                        new RateLimitProperties.Rule("/api/v1/auth/register", 3, 1)
                )
        );
        return new RateLimitFilter(props, objectMapper);
    }

    private MockHttpServletResponse send(RateLimitFilter filter, String method, String uri, String ip)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("Permite 5 intentos de login por minuto y bloquea el sexto con HTTP 429")
    void blocksSixthLoginAttempt() throws Exception {
        RateLimitFilter filter = filter(true);

        for (int i = 1; i <= 5; i++) {
            MockHttpServletResponse ok = send(filter, "POST", LOGIN, "10.0.0.1");
            assertThat(ok.getStatus()).as("intento %d", i).isEqualTo(200);
            assertThat(ok.getHeader(RateLimitFilter.HEADER_REMAINING)).isEqualTo(String.valueOf(5 - i));
        }

        MockHttpServletResponse blocked = send(filter, "POST", LOGIN, "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader(RateLimitFilter.HEADER_RETRY_AFTER)).isNotBlank();
        assertThat(Long.parseLong(blocked.getHeader(RateLimitFilter.HEADER_RETRY_AFTER))).isPositive();
        assertThat(blocked.getContentType()).startsWith("application/json");
        assertThat(blocked.getContentAsString()).contains("\"status\":429").contains(LOGIN);
    }

    @Test
    @DisplayName("Cada IP cliente dispone de un bucket independiente")
    void bucketsAreIsolatedPerIp() throws Exception {
        RateLimitFilter filter = filter(true);
        for (int i = 0; i < 5; i++) {
            send(filter, "POST", LOGIN, "10.0.0.1");
        }
        assertThat(send(filter, "POST", LOGIN, "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(send(filter, "POST", LOGIN, "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Cada regla aplica su propio limite (register: 3 por minuto)")
    void eachRuleHasItsOwnLimit() throws Exception {
        RateLimitFilter filter = filter(true);
        for (int i = 0; i < 3; i++) {
            assertThat(send(filter, "POST", "/api/v1/auth/register", "10.0.0.3").getStatus()).isEqualTo(200);
        }
        assertThat(send(filter, "POST", "/api/v1/auth/register", "10.0.0.3").getStatus()).isEqualTo(429);
        assertThat(send(filter, "POST", LOGIN, "10.0.0.3").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Las rutas sin regla configurada no se limitan")
    void unmatchedPathIsNotLimited() throws Exception {
        RateLimitFilter filter = filter(true);
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = send(filter, "GET", "/api/v1/assets", "10.0.0.4");
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader(RateLimitFilter.HEADER_LIMIT)).isNull();
        }
    }

    @Test
    @DisplayName("Las peticiones CORS preflight (OPTIONS) no consumen tokens")
    void preflightIsNotLimited() throws Exception {
        RateLimitFilter filter = filter(true);
        for (int i = 0; i < 10; i++) {
            assertThat(send(filter, "OPTIONS", LOGIN, "10.0.0.5").getStatus()).isEqualTo(200);
        }
        assertThat(send(filter, "POST", LOGIN, "10.0.0.5").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Con bucket4j.enabled=false el filtro no limita")
    void disabledFilterDoesNotLimit() throws Exception {
        RateLimitFilter filter = filter(false);
        for (int i = 0; i < 10; i++) {
            assertThat(send(filter, "POST", LOGIN, "10.0.0.6").getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("La IP se toma de X-Real-IP o de la ultima entrada de X-Forwarded-For")
    void resolvesClientIpFromTrustedProxyHeaders() {
        RateLimitFilter filter = filter(true);

        MockHttpServletRequest withRealIp = new MockHttpServletRequest("POST", LOGIN);
        withRealIp.addHeader("X-Real-IP", "203.0.113.7");
        withRealIp.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.7");
        assertThat(filter.resolveClientIp(withRealIp)).isEqualTo("203.0.113.7");

        MockHttpServletRequest spoofed = new MockHttpServletRequest("POST", LOGIN);
        spoofed.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.9");
        assertThat(filter.resolveClientIp(spoofed)).isEqualTo("198.51.100.9");

        MockHttpServletRequest direct = new MockHttpServletRequest("POST", LOGIN);
        direct.setRemoteAddr("192.0.2.1");
        assertThat(filter.resolveClientIp(direct)).isEqualTo("192.0.2.1");
    }
}
