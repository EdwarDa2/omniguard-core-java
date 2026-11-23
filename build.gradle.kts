plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.7"
    id("com.diffplug.spotless") version "6.25.0"
    id("org.owasp.dependencycheck") version "9.0.1"
    id("com.google.cloud.tools.jib") version "3.4.0"
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
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    named("integrationTestImplementation") {
        extendsFrom(configurations.testImplementation.get())
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
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
    // Seguridad & Criptografía (Argon2id vía BouncyCastle)
    // ============================================================
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

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

    // Integration test specific
    "integrationTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "integrationTestImplementation"("org.testcontainers:junit-jupiter:1.19.3")
    "integrationTestImplementation"("org.testcontainers:postgresql:1.19.3")
    "integrationTestImplementation"("org.testcontainers:redis:1.19.3")

    // ============================================================
    // DevTools (optional, solo desarrollo)
    // ============================================================
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation", "-parameters"))
}

// ============================================================
// Checkstyle (Google Java Style)
// ============================================================
checkstyle {
    toolVersion = "10.17.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = true
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// ============================================================
// SpotBugs
// ============================================================
spotbugs {
    toolVersion.set("4.8.5")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
    ignoreFailures.set(true)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("xml") {
            required.set(true)
        }
        create("html") {
            required.set(true)
        }
    }
}

// ============================================================
// Spotless (formato de código)
// ============================================================
spotless {
    java {
        googleJavaFormat("1.23.0")
        licenseHeader(file("config/spotless/license-header.txt").readText())
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
    failBuildOnCVSS = 7.0f
    suppressionFile = "config/dependency-check/suppression.xml"
    formats = listOf("HTML", "JSON", "SARIF")
    outputDirectory = "build/reports/dependency-check"
}

// ============================================================
// Integration Test Task
// ============================================================
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests with Testcontainers"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "integration-test")
    shouldRunAfter(tasks.test)
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.check {
    dependsOn(tasks.named("integrationTest"))
}

// ============================================================
// Docker Build (Jib)
// ============================================================
jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
    }
    to {
        image = "ghcr.io/${project.group}/${project.name}"
        tags = setOf(project.version.toString(), "latest")
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