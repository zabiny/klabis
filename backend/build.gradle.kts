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
val mapstructSpringExtensionsVersion = "2.0.0"
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

    // MapStruct Spring Extensions (loose coupling between Spring-managed mappers via ConversionService)
    implementation("org.mapstruct.extensions.spring:mapstruct-spring-annotations:$mapstructSpringExtensionsVersion")
    annotationProcessor("org.mapstruct.extensions.spring:mapstruct-spring-extensions:$mapstructSpringExtensionsVersion")
    testAnnotationProcessor("org.mapstruct.extensions.spring:mapstruct-spring-extensions:$mapstructSpringExtensionsVersion")
    testImplementation("org.mapstruct.extensions.spring:mapstruct-spring-test-extensions:$mapstructSpringExtensionsVersion")

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

    // JsonNullable: tri-state (absent / null / value) wrapper for PATCH request bodies.
    // 0.2.10+ ships a Jackson 3 module (tools.jackson); the Jackson 2 artifacts it also declares
    // stay off the classpath because both are `provided` upstream.
    implementation("org.openapitools:jackson-databind-nullable:0.2.11")

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
// The API is spec-first: docs/openapi/spec/ is the source of truth and `openapiBundle` produces the
// committed klabis-full.json. This task is kept only to dump what the running application actually
// serves, into gitignored docs/openapi/generated/, for ad-hoc comparison against the spec. Nothing
// in the build depends on its output.
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
// Spec bundling (tools/openapi-bundle)
// ---------------------------------------------------------------------------

val openapiToolDir = layout.projectDirectory.dir("../tools/openapi-bundle")

// IDE-launched Gradle daemons (e.g. IntelliJ) don't inherit the interactive shell's PATH, so a
// bare "node" fails with "A problem occurred starting process 'command 'node''" when Node is
// only available via nvm. run-node.sh sources nvm itself (nvm use 24) before exec'ing node, so
// it works no matter what environment launched Gradle.
val runNodeScript = openapiToolDir.file("run-node.sh").asFile.absolutePath

/**
 * Produces the committed klabis-full.json from docs/openapi/spec/, which the frontend's
 * `npm run openapi` reads to generate its TypeScript types.
 *
 * Writes by default rather than only validating: this is now the sole producer of the file, so
 * `./gradlew openapiBundle` has to be enough to refresh it. Pass -PopenapiOut to redirect it, or
 * -PopenapiCheck to validate without writing (what CI wants).
 */
val openapiBundle by tasks.registering(Exec::class) {
    group = "openapi"
    description = "Bundles the hand-written OpenAPI spec into docs/openapi/klabis-full.json"
    workingDir = openapiToolDir.asFile
    val bundleArgs = when {
        project.hasProperty("openapiCheck") && project.hasProperty("openapiOut") ->
            throw GradleException("openapiCheck and openapiOut are mutually exclusive: "
                + "-PopenapiCheck writes nothing, so -PopenapiOut would have no effect")
        project.hasProperty("openapiCheck") -> arrayOf("--check")
        project.hasProperty("openapiOut") -> arrayOf("--out", project.property("openapiOut").toString())
        else -> emptyArray()
    }
    commandLine(runNodeScript, "bundle.mjs", *bundleArgs)
}

// ---------------------------------------------------------------------------
// Java DTO + API interface codegen from docs/openapi/spec/
//
// One generate task PER MODULE, registered via the openApiModule(...) extension function
// (buildSrc/src/main/kotlin/OpenApiModule.kt). A single shared task cannot serve more than one
// module: modelPackage/apiPackage are scalars, and schemaMappings is global per task — two modules
// each defining their own (say) AddressRequest would collide irreconcilably. Per-module tasks keep
// each module's mappings in its own namespace, which is what makes this scale past the two pilot
// modules.
//
// Each module's specFile points at its own docs/openapi/spec/<file>.yaml — one file, one Gradle
// task, no models/apis enumeration; the target file's full paths/components.schemas content is the
// module's generation scope.
// ---------------------------------------------------------------------------

openApiModule(
    module = "members",
    pkg = "com.klabis.members.infrastructure.restapi",
    specFile = "members.yaml",
    mappings = emptyMap()
)

openApiModule(
    module = "finance",
    pkg = "com.klabis.finance.infrastructure.restapi",
    specFile = "finance.yaml",
    mappings = emptyMap()
)

openApiModule(
    module = "events",
    pkg = "com.klabis.events.infrastructure.restapi",
    specFile = "events.yaml",
    mappings = emptyMap()
)

openApiModule(
    module = "calendar",
    pkg = "com.klabis.calendar.infrastructure.restapi",
    specFile = "calendar.yaml",
    mappings = emptyMap()
)

openApiModule(
    module = "membershipfees",
    pkg = "com.klabis.membershipfees.infrastructure.restapi",
    specFile = "membershipfees.yaml",
    mappings = emptyMap()
)

openApiModule(
    module = "groups",
    pkg = "com.klabis.groups.infrastructure.restapi",
    specFile = "groups.yaml",
    // mappings and imports needed because of using EntityModel<T> as 2nd level attribute in responses (group.members, etc.. )
    mappings = mapOf(
        "EntityModelParentResponse" to "org.springframework.hateoas.EntityModel<ParentResponse>",
        "EntityModelFamilyGroupMembershipResponse" to "org.springframework.hateoas.EntityModel<FamilyGroupMembershipResponse>",
        "EntityModelOwnerResponse" to "org.springframework.hateoas.EntityModel<OwnerResponse>",
        "EntityModelFreeGroupMembershipResponse" to "org.springframework.hateoas.EntityModel<FreeGroupMembershipResponse>",
        "EntityModelPendingInvitationResponse" to "org.springframework.hateoas.EntityModel<PendingInvitationResponse>",
        "EntityModelTrainerResponse" to "org.springframework.hateoas.EntityModel<TrainerResponse>",
        "EntityModelGroupMembershipResponse" to "org.springframework.hateoas.EntityModel<GroupMembershipResponse>"
    ),
    extraImportMappings = mapOf(
        "org.springframework.hateoas.EntityModel<ParentResponse>" to "org.springframework.hateoas.EntityModel",
        "org.springframework.hateoas.EntityModel<FamilyGroupMembershipResponse>" to "org.springframework.hateoas.EntityModel",
        "org.springframework.hateoas.EntityModel<OwnerResponse>" to "org.springframework.hateoas.EntityModel",
        "org.springframework.hateoas.EntityModel<FreeGroupMembershipResponse>" to "org.springframework.hateoas.EntityModel",
        "org.springframework.hateoas.EntityModel<PendingInvitationResponse>" to "org.springframework.hateoas.EntityModel",
        "org.springframework.hateoas.EntityModel<TrainerResponse>" to "org.springframework.hateoas.EntityModel",
        "org.springframework.hateoas.EntityModel<GroupMembershipResponse>" to "org.springframework.hateoas.EntityModel"
    )
)

openApiModule(
    module = "common",
    pkg = "com.klabis.common.users.infrastructure.restapi",
    specFile = "common.yaml",
    mappings = mapOf(
        "EntityModelRootModel" to "com.klabis.common.ui.RootModel",
        "EntityModelDashboardModel" to "com.klabis.common.ui.DashboardModel"
    )
)

openApiModule(
    module = "oris",
    pkg = "com.klabis.oris",
    specFile = "oris.yaml",
    mappings = emptyMap()
)
