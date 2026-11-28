# Acerca de omniguard-core-java

## Descripción General

`omniguard-core-java` es el núcleo transaccional, de gobierno de datos y de gestión de identidades (IAM) de la plataforma **OmniGuard SOC**. Desarrollado en **Java 21** y **Spring Boot 3.2**, aplica estrictamente la **Arquitectura Hexagonal (Puertos y Adaptadores)** para desacoplar las reglas de negocio de los mecanismos de persistencia, caché y transporte HTTP.

Implementa un esquema de seguridad defensiva basado en tokens criptográficos **PASETO v4 local** (`v4.local`) aislados exclusivamente en cookies `HttpOnly; SameSite=Strict`, cifrado de credenciales con **Argon2id** (BouncyCastle), control de acceso basado en roles (RBAC), limitación de tasa distribuida mediante **Bucket4j + Redis** y transmisión de alertas en vivo hacia el cliente web mediante **Server-Sent Events (SSE)**.

## Contexto del Proyecto

* **Plataforma:** OmniGuard SOC (Security Operations Center Distribuido)
* **Capa Arquitectónica:** Core Backend Empresarial y Orquestación Segura
* **Tecnologías Principales:** Java 21, Spring Boot 3.2.2, Spring Security 6, PASETO v4, Argon2id, PostgreSQL 16, Flyway, Redis, Bucket4j
* **Licencia:** MIT License
* **Institución:** Universidad Politécnica de Chiapas — Ingeniería en Tecnologías de la Información e Innovación Digital
* **Autores:** Edwar Daniel Gonzalez Hernandez, Andres Eduardo Gonzalez Ruiz

---

## Metadatos para la sección "About" de GitHub

Copiar y pegar estos valores en el panel lateral derecho (icono de engranaje en **About**) del repositorio en GitHub:

* **Description:**
  `Nucleo transaccional y API REST de OmniGuard SOC en Java 21 y Spring Boot 3.2 bajo Arquitectura Hexagonal. Seguridad con PASETO v4, Argon2id, RBAC, Bucket4j y Server-Sent Events.`
* **Topics:**
  `java`, `spring-boot`, `hexagonal-architecture`, `paseto`, `argon2`, `cybersecurity`, `rbac`, `sse`, `soc`, `omniguard`
