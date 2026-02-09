buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(platform("org.jmolecules:jmolecules-bom:2025.0.2"))
        classpath("org.jmolecules.integrations:jmolecules-bytebuddy")
    }
}

plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.18.4"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
    jacoco
}

group = "com.klabis"
version = "0.1.0-SNAPSHOT"
description = "Backend API for orienteering club management system"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val mapstructVersion = "1.5.5.Final"
val testcontainersVersion = "1.19.3"
val springModulithVersion = "1.4.6"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.1")
        mavenBom("org.jmolecules:jmolecules-bom:2025.0.2")
    }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Authorization Server for OAuth2
    implementation("org.springframework.security:spring-security-oauth2-authorization-server")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core:$springModulithVersion")
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc:$springModulithVersion")
    implementation("org.springframework.modulith:spring-modulith-events-api:$springModulithVersion")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator:$springModulithVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Flyway for database migrations
    implementation("org.flywaydb:flyway-core")

    // Lombok (annotation processor - must be before MapStruct)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // MapStruct for DTO mapping (annotation processor - must be after Lombok)
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // Jasypt for encryption (GDPR - rodne cislo)
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")

    // SpringDoc OpenAPI (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15")

    // Spring Cloud Resilience4j for rate limiting
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    // Caffeine cache for per-key rate limiting
    implementation("com.github.ben-manes.caffeine:caffeine")

    // jMolecules: DDD and hexagonal architecture annotations
    implementation("org.jmolecules:jmolecules-ddd")
    implementation("org.jmolecules:jmolecules-hexagonal-architecture")
    implementation("org.jmolecules.integrations:jmolecules-spring")
    compileOnly("org.jmolecules.integrations:jmolecules-bytebuddy-nodep")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test:$springModulithVersion")
    testImplementation("org.springframework.modulith:spring-modulith-junit:$springModulithVersion")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.jmolecules.integrations:jmolecules-archunit")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

byteBuddy {
    transformation {
        plugin = org.jmolecules.bytebuddy.JMoleculesPlugin::class.java
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.modulith.test.file-modification-detector", "default")
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    classpath = classpath.filter { !it.name.contains("lombok") }
}

// SpringDoc OpenAPI Gradle Plugin configuration
openApi {
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")
    outputDir.set(file("../docs/openapi"))
    outputFileName.set("klabis-full.json")
    waitTimeInSeconds.set(30)
    customBootRun {
        args.set(listOf("--server.ssl.enabled=false", "--server.port=8080"))
    }
}
