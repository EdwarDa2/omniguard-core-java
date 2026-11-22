package com.omniguard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.domain:https://localhost}")
    private String domain;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniGuard SOC API")
                        .version("v1")
                        .description("""
                            API REST para la plataforma OmniGuard SOC.
                            
                            **Autenticación:** PASETO v4 (cookies HttpOnly)
                            - Access Token: v4.local (15 min)
                            - Refresh Token: v4.public (30 días)
                            
                            **Endpoints públicos:**
                            - `POST /api/v1/auth/login`
                            - `POST /api/v1/auth/register`
                            - `POST /api/v1/auth/refresh`
                            - `GET /actuator/health`
                            
                            **Rate Limits:**
                            - Auth endpoints: 5 req/min/IP
                            - API general: 100 req/min/user
                            - SSE stream: 10 req/min/user
                            
                            **Tiempo real:** SSE en `GET /api/v1/alerts/stream`
                            """)
                        .license(new License().name("Proprietary").url("https://omniguard.local/license")))
                .servers(List.of(
                        new Server().url(domain).description("Production"),
                        new Server().url("http://localhost:8080").description("Development")
                ))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("at")
                                .description("PASETO Access Token en cookie HttpOnly 'at'"))
                        .addSecuritySchemes("refreshTokenAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("rt")
                                .description("PASETO Refresh Token en cookie HttpOnly 'rt' (solo en /api/v1/auth/refresh)")));
    }
}