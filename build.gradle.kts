plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    kotlin("kapt") version "2.3.21"
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.11.0"
}

spotless {
    kotlin {
        ktlint("1.8.0")
        target("src/**/*.kt")
    }
    kotlinGradle {
        ktlint("1.8.0")
        target("*.gradle.kts")
    }
}

group = "io.waggle"
version = "0.0.1-SNAPSHOT"
description = "WaggleAPIServer"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ID Generator
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // AWS S3
    implementation(platform("software.amazon.awssdk:bom:2.59.1"))
    implementation("software.amazon.awssdk:s3")

    // OpenAPI (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // QueryDSL
    // 원본 com.querydsl은 유지보수가 멈춰 CVE-2024-49203이 고쳐지지 않아 OpenFeign 포크 사용
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.7")
    kapt("io.github.openfeign.querydsl:querydsl-apt:7.7:jpa")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j_jdk17-core:8.22.0")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Kotlin
    implementation("tools.jackson.module:jackson-module-kotlin")
    // swagger-core는 Jackson 2로 스키마를 만들어, springdoc이 이 모듈을 감지해야 non-null 프로퍼티를 required로 표시함
    // 빠지면 빌드와 테스트는 통과하고 API 문서에서만 required가 사라짐
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
