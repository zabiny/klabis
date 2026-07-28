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
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.18.4"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
    id("org.openapi.generator") version "7.18.0"
    jacoco
}

group = "com.klabis"
version = "0.1.0-SNAPSHOT"
description = "Backend API for orienteering club management system"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://gitlab.polach.cloud/api/v4/groups/zbm/-/packages/maven")
        name = "gitlab-zbm"
        credentials(HttpHeaderCredentials::class) {
            name = "Private-Token"
            value = providers.gradleProperty("gitlabZbmToken")
                .orElse(providers.environmentVariable("GITLAB_ZBM_TOKEN"))
                .get()
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

val mapstructVersion = "1.6.3"
val testcontainersVersion = "1.19.3"
val springModulithVersion = "2.0.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
        mavenBom("org.jmolecules:jmolecules-bom:2025.0.2")
    }
}

val mockitoAgent: Configuration by configurations.creating

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")

    // ORIS API client
    implementation("com.dpolach.api:oris-client:0db715d9-SNAPSHOT")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core:$springModulithVersion")
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc:$springModulithVersion")
    implementation("org.springframework.modulith:spring-modulith-events-api:$springModulithVersion")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator:$springModulithVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Flyway for database migrations
    runtimeOnly("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    // Lombok (annotation processor - must be before MapStruct)
    implementation("org.flywaydb:flyway-database-postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // MapStruct for DTO mapping (annotation processor - must be after Lombok)
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // RecordBuilder for type-safe record builders
    compileOnly("io.soabase.record-builder:record-builder-core:44")
    annotationProcessor("io.soabase.record-builder:record-builder-processor:44")

    // Jasypt for encryption (GDPR - rodne cislo)
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")

    // SpringDoc OpenAPI (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // Spring Cloud Resilience4j for rate limiting
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    // Caffeine cache for per-key rate limiting
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Apache Commons Validator for IBAN validation
    implementation("commons-validator:commons-validator:1.7")

    // Apache Commons CSV for accommodation list CSV export
    implementation("org.apache.commons:commons-csv:1.13.0")

    // jMolecules: DDD and hexagonal architecture annotations
    implementation("org.jmolecules:jmolecules-ddd")
    implementation("org.jmolecules:jmolecules-hexagonal-architecture")
    implementation("org.jmolecules.integrations:jmolecules-spring")
    compileOnly("org.jmolecules.integrations:jmolecules-bytebuddy-nodep")

    // Development tools (excluded from production JAR automatically)
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // While we are in development phase, let's have it also on production. After migrate to postgres, make this as developmentOnly again
    runtimeOnly("org.springframework.boot:spring-boot-h2console")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.4")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.4")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test:$springModulithVersion")
    testImplementation("org.springframework.modulith:spring-modulith-junit:$springModulithVersion")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.jmolecules.integrations:jmolecules-archunit")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-restclient-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

byteBuddy {
    transformation {
        plugin = org.jmolecules.bytebuddy.JMoleculesPlugin::class.java
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.modulith.test.file-modification-detector", "default")
    systemProperty("spring.test.context.cache.maxSize", "60")
    val testTmpDir = layout.buildDirectory.dir("tmp/test").get().asFile
    doFirst { testTmpDir.mkdirs() }
    systemProperty("java.io.tmpdir", testTmpDir.absolutePath)
    jvmArgs("-Xmx2g", "-javaagent:${mockitoAgent.asPath}")
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

// Zahrne developer dokumentaci z ../docs/developerManual do výsledného JARu
// na classpath:/static/docs/developers/, čímž ji Spring Boot servíruje na /docs/developers/*.
tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory.dir("../docs/developerManual")) {
        into("static/docs/developers")
    }
}

// SpringDoc OpenAPI Gradle Plugin configuration
//
// Output goes to docs/openapi/generated/ (gitignored), not straight to klabis-full.json.
// During the spec-first migration this is only an input for `openapiDriftCheck`; the committed
// klabis-full.json is produced by `copyGeneratedOpenApiSpec` below and will be taken over by
// `openapiBundle` once every module is migrated.
openApi {
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")
    outputDir.set(file("../docs/openapi/generated"))
    outputFileName.set("klabis-codefirst.json")
    waitTimeInSeconds.set(30)
    customBootRun {
        args.set(listOf("--server.ssl.enabled=false", "--server.port=8080", "--jasypt.encryptor.password=something"))
    }
}

// ---------------------------------------------------------------------------
// Spec-first migration tooling (tools/openapi-bundle)
//
// None of these tasks are wired into `build` yet — the API is still code-first and
// klabis-full.json still originates from springdoc. See the migration plan.
// ---------------------------------------------------------------------------

val openapiToolDir = layout.projectDirectory.dir("../tools/openapi-bundle")
val codeFirstSpec = layout.projectDirectory.file("../docs/openapi/generated/klabis-codefirst.json")
val bundledSpec = layout.projectDirectory.file("../docs/openapi/klabis-full.json")

/**
 * Preserves today's behaviour: springdoc output becomes the committed klabis-full.json.
 *
 * generateOpenApiDocs used to write klabis-full.json directly; now that it writes to generated/,
 * this task restores the end result so the existing manual workflow ("regenerate the spec, then
 * run npm run openapi") keeps working. It is finalizedBy generateOpenApiDocs so that developers
 * and scripts calling the old task name still get the file they expect.
 *
 * Removed in the final migration phase, when openapiBundle takes over klabis-full.json.
 */
val copyGeneratedOpenApiSpec by tasks.registering(Copy::class) {
    group = "openapi"
    description = "Copies the springdoc output to docs/openapi/klabis-full.json (code-first, transitional)"
    from(codeFirstSpec)
    into(bundledSpec.asFile.parentFile)
    rename { "klabis-full.json" }
}

tasks.named("generateOpenApiDocs") {
    finalizedBy(copyGeneratedOpenApiSpec)
}

/** Validates docs/openapi/spec/ and bundles it into a single document. */
val openapiBundle by tasks.registering(Exec::class) {
    group = "openapi"
    description = "Bundles the hand-written OpenAPI spec into a single document"
    workingDir = openapiToolDir.asFile
    commandLine("node", "bundle.mjs", *(project.findProperty("openapiOut")
        ?.let { arrayOf("--out", it.toString()) } ?: arrayOf("--check")))
}

/** Reports which operations are not migrated to the hand-written spec yet. */
val openapiDriftCheck by tasks.registering(Exec::class) {
    group = "openapi"
    description = "Compares the springdoc output against the hand-written spec (migration aid)"
    dependsOn(tasks.named("generateOpenApiDocs"))
    workingDir = openapiToolDir.asFile
    commandLine("node", "drift.mjs", *(project.findProperty("openapiModule")
        ?.let { arrayOf("--module", it.toString()) } ?: emptyArray()))
}

// ---------------------------------------------------------------------------
// Java DTO codegen from docs/openapi/spec/ (migration phase 3, members module only)
//
// Generates MemberDetailsResponse and its nested response DTOs into
// com.klabis.members.infrastructure.restapi (same package as the hand-written controller/mapper —
// several cross-module link processors depend on that package via the "members.rest" named
// interface). Domain enums (Gender, DeactivationReason, DrivingLicenseGroup, TrainerLevel,
// RefereeLevel) are reused as-is via schemaMappings/importMappings rather than regenerated.
// ---------------------------------------------------------------------------

val generatedOpenApiModelsDir = layout.buildDirectory.dir("generated/openapi/members")

val bundleSpecForCodegen by tasks.registering(Exec::class) {
    group = "openapi"
    description = "Bundles docs/openapi/spec/ into a single document for the model generator"
    workingDir = openapiToolDir.asFile
    val out = layout.buildDirectory.file("generated/openapi/bundled.json").get().asFile
    doFirst { out.parentFile.mkdirs() }
    commandLine("node", "bundle.mjs", "--out", out.absolutePath)
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(layout.buildDirectory.file("generated/openapi/bundled.json").map { it.asFile.absolutePath })
    outputDir.set(generatedOpenApiModelsDir.map { it.asFile.absolutePath })
    templateDir.set(layout.projectDirectory.dir("src/main/openapi-templates").asFile.absolutePath)
    modelPackage.set("com.klabis.members.infrastructure.restapi")
    skipValidateSpec.set(true)

    globalProperties.set(
        mapOf(
            "models" to listOf(
                "MemberDetailsResponse",
                "AddressResponse",
                "GuardianDTO",
                "IdentityCardDto",
                "MedicalCourseDto",
                "TrainerLicenseDto",
                "RefereeLicenseDto"
            ).joinToString(","),
            "apis" to "false",
            "supportingFiles" to "false",
            "modelDocs" to "false",
            "modelTests" to "false"
        )
    )

    // Promotes inline enum properties (TrainerLicenseDto.level, RefereeLicenseDto.level) to real,
    // named schemas (TrainerLicenseDto_level, RefereeLicenseDto_level) so schemaMappings below can
    // redirect them to the existing domain enums instead of generating a synthesized inner enum.
    inlineSchemaOptions.set(
        mapOf(
            "RESOLVE_INLINE_ENUMS" to "true"
        )
    )

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "documentationProvider" to "none",
            "openApiNullable" to "false",
            "additionalModelTypeAnnotations" to
                "@io.soabase.recordbuilder.core.RecordBuilder @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @org.springframework.security.authorization.method.HandleAuthorizationDenied(handlerClass = com.klabis.common.security.fieldsecurity.NullDeniedHandler.class)"
        )
    )

    // date-time wire format maps to Instant (not the generator's default OffsetDateTime) to match
    // the rest of the codebase (see backend-patterns: ZonedDateTime/Instant in domain, LocalDate in API).
    typeMappings.set(
        mapOf(
            "DateTime" to "Instant"
        )
    )

    schemaMappings.set(
        mapOf(
            "Gender" to "com.klabis.members.domain.Gender",
            "DeactivationReason" to "com.klabis.members.domain.DeactivationReason",
            "DrivingLicenseGroup" to "com.klabis.members.domain.DrivingLicenseGroup",
            "TrainerLicenseDto_level" to "com.klabis.members.domain.TrainerLevel",
            "RefereeLicenseDto_level" to "com.klabis.members.domain.RefereeLevel"
        )
    )

    importMappings.set(
        mapOf(
            "Gender" to "com.klabis.members.domain.Gender",
            "DeactivationReason" to "com.klabis.members.domain.DeactivationReason",
            "DrivingLicenseGroup" to "com.klabis.members.domain.DrivingLicenseGroup",
            "TrainerLicenseDto_level" to "com.klabis.members.domain.TrainerLevel",
            "RefereeLicenseDto_level" to "com.klabis.members.domain.RefereeLevel",
            "Instant" to "java.time.Instant"
        )
    )
}

tasks.named("openApiGenerate") {
    dependsOn(bundleSpecForCodegen)
}

// Part of the main sourceSet (not a separate one) so Lombok -> MapStruct -> RecordBuilder
// annotation processors run over the generated records exactly as they do for hand-written code.
sourceSets.main {
    java.srcDir(generatedOpenApiModelsDir.map { it.dir("src/main/java") })
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}
