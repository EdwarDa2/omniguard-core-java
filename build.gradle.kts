plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
    id("checkstyle")
    id("spotbugs")
    id("com.diffplug.spotless") version "6.25.0"
    id("org.owasp.dependencycheck") version "9.0.1"
    id("pl.piomin.plugins.pitest") version "1.0.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.omniguard"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io") }  // Para com.github.paseto:paseto-java
}

configurations {
    integrationTestImplementation.extendsFrom(testImplementation)
    integrationTestRuntimeOnly.extendsFrom(testRuntimeOnly)
}

dependencies {
    // ============================================================
    // Spring Boot Starters
    // ============================================================
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // ============================================================
    // Base de datos
    // ============================================================
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")

    // ============================================================
    // Redis (sessions, rate limit, pub/sub)
    // ============================================================
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.lettuce:lettuce-core")

    // ============================================================
    // PASETO (Platform-Agnostic Security Tokens)
    // ============================================================
    implementation("com.github.paseto:paseto-java:4.0.0")
    // Alternativa si paseto-java no está en Maven Central:
    // implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    // implementation("com.google.crypto.tink:tink:1.10.0")

    // ============================================================
    // Argon2 (password hashing)
    // ============================================================
    implementation("com.github.jeremyh:jBCrypt:1.0.0")
    // O mejor: implementation("org.springframework.security:spring-security-crypto")
    // Spring Security ya incluye BCrypt, pero para Argon2id:
    implementation("de.mkammerer:argon2-jvm:2.11")

    // ============================================================
    // Mapping / Serialization
    // ============================================================
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // ============================================================
    // OpenAPI / Swagger
    // ============================================================
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // ============================================================
    // Métricas / Observabilidad
    // ============================================================
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    // ============================================================
    // Rate Limiting (Bucket4j + Redis)
    // ============================================================
    implementation("com.bucket4j:bucket4j-spring-boot3-starter:1.0.0")
    implementation("com.bucket4j:bucket4j-redis:8.12.0")

    // ============================================================
    // Utilidades
    // ============================================================
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.google.guava:guava:32.1.3-jre")

    // ============================================================
    // Testing
    // ============================================================
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:redis:1.19.3")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.15.1")

    // Pact Contract Testing
    testImplementation("au.com.dius.pact.consumer:junit5:4.6.0")
    testImplementation("au.com.dius.pact.provider:junit5:4.6.0")
    testImplementation("au.com.dius.pact.provider:spring:4.6.0")

    // Integration test specific
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-test")
    integrationTestImplementation("org.testcontainers:junit-jupiter:1.19.3")
    integrationTestImplementation("org.testcontainers:postgresql:1.19.3")
    integrationTestImplementation("org.testcontainers:redis:1.19.3")

    // ============================================================
    // DevTools (optional, solo desarrollo)
    // ============================================================
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

tasks.withType(Test) {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation" << "-parameters"
}

// ============================================================
// Checkstyle (Google Java Style)
// ============================================================
checkstyle {
    toolVersion = "10.17.0"
    configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.withType(Checkstyle) {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// ============================================================
// SpotBugs
// ============================================================
spotbugs {
    toolVersion = "4.8.5"
    effort = "max"
    reportLevel = "high"
    excludeFilter = file("config/spotbugs/exclude.xml")
}

tasks.withType(com.github.spotbugs.snom.SpotBugsTask) {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// ============================================================
// Spotless (formato de código)
// ============================================================
spotless {
    java {
        googleJavaFormat("1.23.0")
        licenseHeader file("config/spotless/license-header.txt").readText()
        importOrder()
        removeUnusedImports()
    }
    kotlin {
        ktlint("1.0.0")
    }
    format("misc") {
        target("**/*.yml", "**/*.yaml", "**/*.json", "**/*.md", "**/*.sql")
        trimTrailingWhitespace()
        endWithNewline()
        indentWithSpaces(2)
    }
}

// ============================================================
// OWASP Dependency Check
// ============================================================
dependencyCheck {
    failBuildOnCVSS = 7
    suppressionFile = file("config/dependency-check/suppression.xml")
    formats = ["HTML", "JSON", "SARIF"]
    outputDirectory = file("build/reports/dependency-check")
}

// ============================================================
// Integration Test Task
// ============================================================
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests with Testcontainers"
    group = "verification"
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "integration-test")
    shouldRunAfter(tasks.test)
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

// ============================================================
// Mutation Testing (Pitest)
// ============================================================
tasks.register<pl.piomin.plugins.pitest.PitestTask>("pitest") {
    description = "Runs mutation testing with Pitest"
    group = "verification"
    targetClasses = listOf("com.omniguard.domain.*", "com.omniguard.application.*")
    mutators = listOf(
        "CONDITIONALS_BOUNDARY", "INCREMENTS", "INVERT_NEGS",
        "MATH", "NEGATE_CONDITIONALS", "VOID_METHOD_CALLS",
        "RETURN_VALS", "EXPERIMENTAL_MEMBER_VARIABLE", "EXPERIMENTAL_SWITCH"
    )
    minimumScore = 70
    maximumScore = 100
    outputFormats = listOf("HTML", "XML", "CSV")
    timestampedReports = false
    verbose = true
    threads = 4
    // Exclude generated code and configuration classes
    excludedClasses = listOf(
        "com.omniguard.adapter.out.persistence.entity.*",
        "com.omniguard.adapter.in.rest.dto.*",
        "com.omniguard.adapter.in.rest.mapper.*",
        "com.omniguard.config.*",
        "com.omniguard.domain.vo.*",
        "com.omniguard.domain.exception.*",
        "com.omniguard.domain.model.*"
    )
    // Only mutate domain and application logic
    includedClasses = listOf(
        "com.omniguard.domain.model.*",
        "com.omniguard.application.service.*"
    )
}

tasks.check {
    dependsOn(tasks.integrationTest, tasks.pitest)
}

// ============================================================
// Pact Contract Testing Tasks
// ============================================================
tasks.register<Test>("pactTest") {
    description = "Runs Pact consumer contract tests"
    group = "verification"
    useJUnitPlatform()
    systemProperty("pact.rootDir", "build/pacts")
    systemProperty("pact.writer.overwrite", "true")
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.register<Test>("pactVerify") {
    description = "Runs Pact provider verification tests"
    group = "verification"
    useJUnitPlatform()
    systemProperty("pact.rootDir", "build/pacts")
    systemProperty("pact.verifier.publishResults", "false")
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
    // Run after integration tests to ensure DB is up
    shouldRunAfter(tasks.integrationTest)
}

tasks.check {
    dependsOn(tasks.integrationTest, tasks.pitest, tasks.pactTest)
}

// ============================================================
// Docker Build (Jib)
// ============================================================
plugins {
    id("com.google.cloud.tools.jib") version "3.4.0"
}

jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
    }
    to {
        image = "ghcr.io/${project.group}/${project.name}"
        tags = [project.version.toString(), "latest"]
        auth {
            username = System.getenv("REGISTRY_USERNAME") ?: ""
            password = System.getenv("REGISTRY_PASSWORD") ?: ""
        }
    }
    container {
        jvmFlags = listOf("-XX:MaxRAMPercentage=75.0", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom")
        ports = listOf("8080", "8081")
        environment = mapOf(
            "SPRING_PROFILES_ACTIVE" to "docker",
            "SERVER_PORT" to "8080",
            "MANAGEMENT_SERVER_PORT" to "8081"
        )
        user = "1000:1000"
        labels = mapOf(
            "org.opencontainers.image.title" to "OmniGuard Core API",
            "org.opencontainers.image.version" to project.version.toString(),
            "org.opencontainers.image.source" to "https://github.com/tu-org/omniguard-soc"
        )
    }
}