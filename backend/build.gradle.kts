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

// IDE-launched Gradle daemons (e.g. IntelliJ) don't inherit the interactive shell's PATH, so a
// bare "node" fails with "A problem occurred starting process 'command 'node''" when Node is
// only available via nvm. run-node.sh sources nvm itself (nvm use 24) before exec'ing node, so
// it works no matter what environment launched Gradle.
val runNodeScript = openapiToolDir.file("run-node.sh").asFile.absolutePath

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
    commandLine(runNodeScript, "bundle.mjs", *(project.findProperty("openapiOut")
        ?.let { arrayOf("--out", it.toString()) } ?: arrayOf("--check")))
}

/** Reports which operations are not migrated to the hand-written spec yet. */
val openapiDriftCheck by tasks.registering(Exec::class) {
    group = "openapi"
    description = "Compares the springdoc output against the hand-written spec (migration aid)"
    dependsOn(tasks.named("generateOpenApiDocs"))
    workingDir = openapiToolDir.asFile
    commandLine(runNodeScript, "drift.mjs", *(project.findProperty("openapiModule")
        ?.let { arrayOf("--module", it.toString()) } ?: emptyArray()))
}

// ---------------------------------------------------------------------------
// Java DTO + API interface codegen from docs/openapi/spec/
//
// One generate task PER MODULE, registered via openApiModule(...) below. A single shared task
// cannot serve more than one module: modelPackage/apiPackage are scalars, and schemaMappings is
// global per task — two modules each defining their own (say) AddressRequest would collide
// irreconcilably. Per-module tasks keep each module's mappings in its own namespace, which is what
// makes this scale past the two pilot modules.
//
// Each module generates into the same package as its hand-written controller/mapper, because
// cross-module link processors depend on those packages via Modulith named interfaces.
// ---------------------------------------------------------------------------

val bundleSpecForCodegen by tasks.registering(Exec::class) {
    group = "openapi"
    description = "Bundles docs/openapi/spec/ into a single document for the model generator"
    workingDir = openapiToolDir.asFile
    val out = layout.buildDirectory.file("generated/openapi/bundled.json").get().asFile
    doFirst { out.parentFile.mkdirs() }
    commandLine(runNodeScript, "bundle.mjs", "--out", out.absolutePath)
}

/**
 * Registers a spec-first codegen task for one module and wires its output into the main sourceSet.
 *
 * @param module   short module name; also the build/generated/openapi/<module> output directory
 * @param pkg      target package for both models and APIs
 * @param apis     OpenAPI tags to generate interfaces for. Must be listed explicitly: the "apis"
 *                 global property generates EVERY tag when left empty, so without this filter each
 *                 module's task would emit every other module's *Api.java into its own package.
 * @param models   payload schemas to generate as Java records (envelopes stay spec-only)
 * @param mappings schema name -> existing Java type, for types reused instead of regenerated
 */
fun openApiModule(
    module: String,
    pkg: String,
    apis: List<String>,
    models: List<String>,
    mappings: Map<String, String>,
    extraImportMappings: Map<String, String> = emptyMap()
) {
    val outputDir = layout.buildDirectory.dir("generated/openapi/$module")

    val task = tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(
        "openApiGenerate${module.replaceFirstChar { it.uppercase() }}"
    ) {
        group = "openapi"
        description = "Generates spec-first DTOs and API interfaces for the $module module"
        dependsOn(bundleSpecForCodegen)

        // The generator only writes files; it never removes ones it no longer produces. Without
        // this, a schema renamed or dropped in the spec leaves its old record behind in build/ —
        // and since the directory is on the main sourceSet, that ghost still compiles. Local builds
        // then keep working against a type the spec no longer defines, and only a clean CI build
        // fails.
        doFirst { delete(outputDir) }

        generatorName.set("spring")
        inputSpec.set(layout.buildDirectory.file("generated/openapi/bundled.json").map { it.asFile.absolutePath })
        this.outputDir.set(outputDir.map { it.asFile.absolutePath })
        templateDir.set(layout.projectDirectory.dir("src/main/openapi-templates").asFile.absolutePath)
        modelPackage.set(pkg)
        apiPackage.set(pkg)
        skipValidateSpec.set(true)

        globalProperties.set(
            mapOf(
                "models" to models.joinToString(","),
                // Tag names, NOT "true" — the generator silently generates nothing for "apis"="true",
                // and every tag when given an empty string.
                "apis" to apis.joinToString(","),
                "supportingFiles" to "false",
                "modelDocs" to "false",
                "modelTests" to "false",
                "apiDocs" to "false",
                "apiTests" to "false"
            )
        )

        // Promotes inline enum properties to real, named schemas (e.g. TrainerLicenseDto_level) so
        // schemaMappings can redirect them to existing domain enums instead of generating a
        // synthesized inner enum.
        inlineSchemaOptions.set(mapOf("RESOLVE_INLINE_ENUMS" to "true"))

        configOptions.set(
            mapOf(
                "interfaceOnly" to "true",
                // Plain abstract interface methods only — the stock default-method body returns 501
                // via a non-existent ApiUtil helper and is never used, since the controllers always
                // provide a real @Override.
                "skipDefaultInterface" to "true",
                "useSpringBoot3" to "true",
                "useJakartaEe" to "true",
                "documentationProvider" to "none",
                "openApiNullable" to "false",
                "useTags" to "true",
                "additionalModelTypeAnnotations" to
                    "@io.soabase.recordbuilder.core.RecordBuilder @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @org.springframework.security.authorization.method.HandleAuthorizationDenied(handlerClass = com.klabis.common.security.fieldsecurity.NullDeniedHandler.class)"
            )
        )

        // date-time wire format maps to Instant (not the generator's default OffsetDateTime) to match
        // the rest of the codebase (see backend-patterns: ZonedDateTime/Instant in domain, LocalDate in API).
        typeMappings.set(mapOf("DateTime" to "Instant"))

        // Envelope schemas (EntityModel*/PagedModel*/CollectionModel*) and hand-written request DTOs
        // are redirected to existing Java types rather than regenerated — see klabis-api-spec skill:
        // "Payload and envelope are separate schemas". The API interface signature must match what
        // HalResponseBodyAdvice expects (plain payload DTO / Page<T> / List<T>, see ADR-002).
        val commonMappings = mapOf("ProblemDetail" to "org.springframework.http.ProblemDetail")
        schemaMappings.set(mappings + commonMappings)
        importMappings.set(mappings + commonMappings + extraImportMappings + mapOf("Instant" to "java.time.Instant"))
    }

    // Part of the main sourceSet (not a separate one) so Lombok -> MapStruct -> RecordBuilder
    // annotation processors run over the generated records exactly as they do for hand-written code.
    sourceSets.main { java.srcDir(outputDir.map { it.dir("src/main/java") }) }
    tasks.named("compileJava") { dependsOn(task) }
}

openApiModule(
    module = "members",
    pkg = "com.klabis.members.infrastructure.restapi",
    apis = listOf("Members", "Registration"),
    models = listOf(
        "MemberDetailsResponse",
        "AddressResponse",
        "GuardianDTO",
        "IdentityCardDto",
        "MedicalCourseDto",
        "TrainerLicenseDto",
        "RefereeLicenseDto",
        "RegisterMemberRequest",
        "AddressRequest",
        "SuspendMembershipRequest"
    ),
    mappings = mapOf(
        "Gender" to "com.klabis.members.domain.Gender",
        "DeactivationReason" to "com.klabis.members.domain.DeactivationReason",
        "DrivingLicenseGroup" to "com.klabis.members.domain.DrivingLicenseGroup",
        "TrainerLicenseDto_level" to "com.klabis.members.domain.TrainerLevel",
        "RefereeLicenseDto_level" to "com.klabis.members.domain.RefereeLevel",
        "EntityModelMemberDetailsResponse" to "com.klabis.members.infrastructure.restapi.MemberDetailsResponse",
        "PagedModelEntityModelMemberSummaryResponse" to "org.springframework.data.domain.Page<com.klabis.members.infrastructure.restapi.MemberSummaryResponse>",
        // UpdateMemberRequest stays hand-written — every property is a PatchField<T> wrapper, whose
        // absent/null/value tri-state has no OpenAPI equivalent.
        "UpdateMemberRequest" to "com.klabis.members.infrastructure.restapi.UpdateMemberRequest"
    ),
    // The generic Page<T> mapping above carries type arguments that the import statement must not repeat.
    extraImportMappings = mapOf(
        "PagedModelEntityModelMemberSummaryResponse" to "org.springframework.data.domain.Page"
    )
)

openApiModule(
    module = "event-types",
    pkg = "com.klabis.events.infrastructure.restapi",
    apis = listOf("EventTypes"),
    models = listOf(
        "EventTypeDto",
        "CreateEventTypeRequest",
        "UpdateEventTypeRequest"
    ),
    mappings = mapOf(
        "EntityModelEventTypeDto" to "com.klabis.events.infrastructure.restapi.EventTypeDto",
        // Collection, not List: "List" is a reserved container name in the generator's type system,
        // and a schemaMapping onto it is dropped silently — the method then generates as
        // `ResponseEntity<>`, which fails to compile. Collection carries the same meaning for
        // HalResponseBodyAdvice, which only iterates the value.
        "CollectionModelEntityModelEventTypeDto" to "java.util.Collection<com.klabis.events.infrastructure.restapi.EventTypeDto>"
    ),
    // The generic Collection<T> mapping above carries type arguments that the import must not repeat.
    extraImportMappings = mapOf(
        "CollectionModelEntityModelEventTypeDto" to "java.util.Collection"
    )
)

openApiModule(
    module = "finance",
    pkg = "com.klabis.finance.infrastructure.restapi",
    apis = listOf("Finance"),
    // MemberAccountResource/TransactionResource are hand-written (mapper logic onto Money/Transaction
    // domain types) and stay that way — only the request DTOs are generated fresh.
    models = listOf(
        "DepositRequest",
        "ChargeRequest",
        "ReverseRequest"
    ),
    mappings = mapOf(
        "EntityModelMemberAccountResource" to "com.klabis.finance.infrastructure.restapi.MemberAccountResource",
        "EntityModelTransactionResource" to "com.klabis.finance.infrastructure.restapi.TransactionResource",
        "PagedModelEntityModelTransactionResource" to "org.springframework.data.domain.Page<com.klabis.finance.infrastructure.restapi.TransactionResource>"
    ),
    // The generic Page<T> mapping above carries type arguments that the import must not repeat.
    extraImportMappings = mapOf(
        "PagedModelEntityModelTransactionResource" to "org.springframework.data.domain.Page"
    )
)
openApiModule(
    module = "events",
    pkg = "com.klabis.events.infrastructure.restapi",
    // OrisEvents is its own tag (not "Events") so it gets its own generated interface even though
    // OrisEventController shares the /api/events URL prefix with EventController — see
    // klabis-api-spec skill: "one *Api interface per tag" and events.yaml header comment.
    // "EventRegistrations" replaces the original multi-word @Tag "Event Registrations", which the
    // generator silently drops.
    //
    // Two operations on this tag need a note:
    //   - getEvent returns EventDto; its _embedded.registrationDtoList is contributed by
    //     the controller via HalResponseContext.embed(...) and assembled by HalResponseBodyAdvice,
    //     so the _embedded block never appears in the Java return type.
    //   - the accommodation-list path answers with two produces variants (HAL JSON and text/csv) on
    //     one operation; the generator emits ONE Java method (getAccommodationList) whose inherited
    //     produces lists both content types. The hand-written getAccommodationList (JSON) DOES
    //     implement that generated method — its own @GetMapping narrows produces down to the HAL
    //     variant, because Spring refuses to dispatch when getAccommodationList and the separate
    //     hand-written getAccommodationListAsCsv (text/csv) both claim to serve text/csv
    //     ("Ambiguous handler methods"). getAccommodationListAsCsv itself has no interface
    //     counterpart — see the comment on that method in EventController.
    // Same precedent as IcalFeedController in the calendar module.
    apis = listOf("Events", "OrisEvents", "EventRegistrations", "CategoryPresets"),
    models = listOf(
        "ImportCommand",
        "ImportBatchRequest",
        "RegisterEventRequest",
        "EditRegistrationRequest",
        "CreateCategoryPresetRequest",
        "UpdateCategoryPresetRequest"
    ),
    mappings = mapOf(
        // CreateEventRequest/UpdateEventRequest stay hand-written — see the comment above
        // CreateEventRequest in events.yaml for why (cross-field @AssertTrue validation on create,
        // PatchField<T> wrappers on update — neither is representable purely in OpenAPI).
        "CreateEventRequest" to "com.klabis.events.infrastructure.restapi.CreateEventRequest",
        "UpdateEventRequest" to "com.klabis.events.infrastructure.restapi.UpdateEventRequest",
        "EventStatus" to "com.klabis.events.domain.EventStatus",
        "EntityModelEventSummaryDto" to "com.klabis.events.infrastructure.restapi.EventSummaryDto",
        "PagedModelEntityModelEventSummaryDto" to "org.springframework.data.domain.Page<com.klabis.events.infrastructure.restapi.EventSummaryDto>",
        // getEvent returns the payload; the _embedded.registrationDtoList block in the
        // WithRegistrations schema is contributed by the controller via HalResponseContext.embed(...)
        // and assembled by HalResponseBodyAdvice, so it does not belong in the Java return type.
        "EntityModelEventDtoWithRegistrations" to "com.klabis.events.infrastructure.restapi.EventDto",
        "CollectionModelEntityModelAccommodationListItemDto" to "java.util.Collection<com.klabis.events.infrastructure.restapi.AccommodationListItemDto>",
        "EntityModelBulkSyncResult" to "com.klabis.events.application.BulkSyncResult",
        "EntityModelBulkImportResult" to "com.klabis.events.application.BulkImportResult",
        "EntityModelRegistrationDto" to "com.klabis.events.infrastructure.restapi.RegistrationDto",
        // Collection, not List: "List" is a reserved container name in the generator's type system,
        // and a schemaMapping onto it is dropped silently — the method then generates as
        // `ResponseEntity<>`, which fails to compile.
        "CollectionModelEntityModelRegistrationSummaryDto" to "java.util.Collection<com.klabis.events.infrastructure.restapi.RegistrationSummaryDto>",
        "EntityModelCategoryPresetDto" to "com.klabis.events.infrastructure.restapi.CategoryPresetDto",
        "CollectionModelEntityModelCategoryPresetDto" to "java.util.Collection<com.klabis.events.infrastructure.restapi.CategoryPresetDto>"
    ),
    // The generic Page<T>/Collection<T>/CollectionModel<T> mappings above carry type arguments that
    // the import statement must not repeat.
    extraImportMappings = mapOf(
        "PagedModelEntityModelEventSummaryDto" to "org.springframework.data.domain.Page",
        "CollectionModelEntityModelAccommodationListItemDto" to "java.util.Collection",
        "CollectionModelEntityModelRegistrationSummaryDto" to "java.util.Collection",
        "CollectionModelEntityModelCategoryPresetDto" to "java.util.Collection"
    )
)

openApiModule(
    module = "calendar",
    pkg = "com.klabis.calendar.infrastructure.restapi",
    apis = listOf("Calendar", "IcalToken", "IcalFeed"),
    // CalendarItemDto and IcalTokenResponse are hand-written; only the request DTOs are generated.
    models = listOf(
        "CreateCalendarItemRequest",
        "UpdateCalendarItemRequest"
    ),
    mappings = mapOf(
        "EntityModelCalendarItemDto" to "com.klabis.calendar.infrastructure.restapi.CalendarItemDto",
        "EntityModelIcalTokenResponse" to "com.klabis.calendar.infrastructure.restapi.IcalTokenResponse",
        // Collection, not List — see the event-types block above for why List is unusable here.
        "CollectionModelEntityModelCalendarItemDto" to "java.util.Collection<com.klabis.calendar.infrastructure.restapi.CalendarItemDto>"
    ),
    extraImportMappings = mapOf(
        "CollectionModelEntityModelCalendarItemDto" to "java.util.Collection"
    )
)

openApiModule(
    module = "membershipfees",
    pkg = "com.klabis.membershipfees.infrastructure.restapi",
    // getGroup (MembershipFeeGroups) is documented in membershipfees.yaml but deliberately absent
    // from `models`/left unmapped, so MembershipFeeGroupsApi does not declare it and
    // MembershipFeeGroupController keeps its hand-written method: it embeds a second,
    // independently-shaped collection (group members) alongside the main payload via
    // HalModelBuilder — HalResponseContext only supports a single domain object or a flat list.
    // Same precedent as EventController's getEvent in the events module.
    apis = listOf("MembershipFeeTiers", "FeeSelectionCampaigns", "MembershipFeeGroups", "MemberFeeChoice", "MemberFeeSummary"),
    models = listOf(
        "CreateMembershipFeeTierRequest",
        "EditMembershipFeeTierRequest",
        "PaymentRuleRequest",
        "AddPaymentRuleRequest",
        "EditPaymentRuleRequest",
        "PublishYearRequest",
        "ChangeDeadlineRequest",
        "EditGroupSnapshotRequest",
        "AdminAssignMemberRequest",
        "ChooseFeeChoiceRequest"
    ),
    mappings = mapOf(
        "EntityModelMembershipFeeTierSummaryResponse" to "com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierSummaryResponse",
        "CollectionModelEntityModelMembershipFeeTierSummaryResponse" to "java.util.Collection<com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierSummaryResponse>",
        "EntityModelMembershipFeeTierResponse" to "com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierResponse",
        "EntityModelPaymentRuleResponse" to "com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierResponse.PaymentRuleResponse",
        "CollectionModelEntityModelPaymentRuleResponse" to "java.util.Collection<com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierResponse.PaymentRuleResponse>",
        "EntityModelFeeSelectionCampaignResponse" to "com.klabis.membershipfees.infrastructure.restapi.FeeSelectionCampaignResponse",
        "CollectionModelEntityModelFeeSelectionCampaignResponse" to "java.util.Collection<com.klabis.membershipfees.infrastructure.restapi.FeeSelectionCampaignResponse>",
        "EntityModelMembershipFeeGroupResponse" to "com.klabis.membershipfees.infrastructure.restapi.MembershipFeeGroupResponse",
        "CollectionModelEntityModelMembershipFeeGroupResponse" to "java.util.Collection<com.klabis.membershipfees.infrastructure.restapi.MembershipFeeGroupResponse>",
        // getGroup returns the payload; the _embedded.members block in the WithMembers schema is
        // contributed by the controller via HalResponseContext.embed(...) and assembled by
        // HalResponseBodyAdvice, so it does not belong in the Java return type.
        "EntityModelMembershipFeeGroupResponseWithMembers" to "com.klabis.membershipfees.infrastructure.restapi.MembershipFeeGroupResponse",
        "EntityModelMemberFeeChoiceResponse" to "com.klabis.membershipfees.infrastructure.restapi.MemberFeeChoiceResponse",
        "EntityModelMemberFeeSummaryResponse" to "com.klabis.membershipfees.infrastructure.restapi.MemberFeeSummaryResponse",
        "EntityModelMemberFeeHistoryResponse" to "com.klabis.membershipfees.infrastructure.restapi.MemberFeeHistoryResponse"
    ),
    // The generic Collection<T> mappings above carry type arguments that the import statement must
    // not repeat.
    extraImportMappings = mapOf(
        "CollectionModelEntityModelMembershipFeeTierSummaryResponse" to "java.util.Collection",
        "CollectionModelEntityModelPaymentRuleResponse" to "java.util.Collection",
        "CollectionModelEntityModelFeeSelectionCampaignResponse" to "java.util.Collection",
        "CollectionModelEntityModelMembershipFeeGroupResponse" to "java.util.Collection"
    )
)

// The groups module spans THREE Java packages (familygroup/freegroup/traininggroup), each with its
// own controller — openApiModule's pkg/models/mappings are scalar-per-task, so this is three
// registrations against the ONE groups.yaml spec file, not one. See groups.yaml header comment.
openApiModule(
    module = "groupsFamily",
    pkg = "com.klabis.groups.familygroup.infrastructure.restapi",
    // getFamilyGroup IS generated onto FamilyGroupsApi; only its response schema is
    // documentation-only, because the record's parents/members are List<EntityModel<X>> whose items
    // carry their own _links. See groups.yaml header comment and the comment on that method in
    // FamilyGroupController.
    apis = listOf("FamilyGroups"),
    models = listOf(
        "CreateFamilyGroupRequest",
        "AddMemberRequest"
    ),
    mappings = mapOf(
        "EntityModelFamilyGroupSummaryResponse" to "com.klabis.groups.familygroup.infrastructure.restapi.FamilyGroupSummaryResponse",
        "CollectionModelEntityModelFamilyGroupSummaryResponse" to "java.util.Collection<com.klabis.groups.familygroup.infrastructure.restapi.FamilyGroupSummaryResponse>",
        // Payload type for getFamilyGroup, same envelope-stripping mapping as every other
        // EntityModelX -> X above. The record's parents/members fields stay List<EntityModel<X>>
        // (per-item _links), which is why the record is hand-written rather than generated.
        "EntityModelFamilyGroupResponse" to "com.klabis.groups.familygroup.infrastructure.restapi.FamilyGroupResponse"
    ),
    extraImportMappings = mapOf(
        "CollectionModelEntityModelFamilyGroupSummaryResponse" to "java.util.Collection"
    )
)

openApiModule(
    module = "groupsFree",
    pkg = "com.klabis.groups.freegroup.infrastructure.restapi",
    // getGroup (Groups) is generated; only its response schema is documentation-only — same reason
    // as getFamilyGroup above. See groups.yaml header comment.
    apis = listOf("Groups", "Invitations"),
    models = listOf(
        "CreateGroupRequest",
        "RenameGroupRequest",
        "AddOwnerRequest",
        "InviteMemberRequest",
        "CancelInvitationRequest"
    ),
    mappings = mapOf(
        "EntityModelGroupSummaryResponse" to "com.klabis.groups.freegroup.infrastructure.restapi.GroupSummaryResponse",
        "CollectionModelEntityModelGroupSummaryResponse" to "java.util.Collection<com.klabis.groups.freegroup.infrastructure.restapi.GroupSummaryResponse>",
        // Payload type for getGroup — same envelope-stripping mapping as every other EntityModelX
        // -> X. The record's owners/members/pendingInvitations stay List<EntityModel<X>>.
        "EntityModelGroupResponse" to "com.klabis.groups.freegroup.infrastructure.restapi.GroupResponse",
        "EntityModelPendingInvitationResponseForInvitationsList" to "com.klabis.groups.freegroup.infrastructure.restapi.PendingInvitationResponse",
        "CollectionModelEntityModelPendingInvitationResponseForInvitationsList" to "java.util.Collection<com.klabis.groups.freegroup.infrastructure.restapi.PendingInvitationResponse>"
    ),
    extraImportMappings = mapOf(
        "CollectionModelEntityModelGroupSummaryResponse" to "java.util.Collection",
        "CollectionModelEntityModelPendingInvitationResponseForInvitationsList" to "java.util.Collection"
    )
)

openApiModule(
    module = "groupsTraining",
    pkg = "com.klabis.groups.traininggroup.infrastructure.restapi",
    // getTrainingGroup is generated; only its response schema is documentation-only — same reason as
    // getFamilyGroup above. See groups.yaml header comment.
    apis = listOf("TrainingGroups"),
    models = listOf(
        "CreateTrainingGroupRequest",
        "TrainingGroupAddMemberRequest",
        "AddTrainerRequest",
        "AgeRangeRequest"
        // UpdateTrainingGroupRequest stays hand-written — every property is a PatchField<T>
        // wrapper, which OpenAPI cannot express (see the comment on that schema in groups.yaml).
    ),
    mappings = mapOf(
        "UpdateTrainingGroupRequest" to "com.klabis.groups.traininggroup.infrastructure.restapi.UpdateTrainingGroupRequest",
        "EntityModelTrainingGroupSummaryResponse" to "com.klabis.groups.traininggroup.infrastructure.restapi.TrainingGroupSummaryResponse",
        "CollectionModelEntityModelTrainingGroupSummaryResponse" to "java.util.Collection<com.klabis.groups.traininggroup.infrastructure.restapi.TrainingGroupSummaryResponse>",
        // Payload type for getTrainingGroup — same envelope-stripping mapping as every other
        // EntityModelX -> X. The record's trainers/members stay List<EntityModel<X>>.
        "EntityModelTrainingGroupResponse" to "com.klabis.groups.traininggroup.infrastructure.restapi.TrainingGroupResponse"
    ),
    extraImportMappings = mapOf(
        "CollectionModelEntityModelTrainingGroupSummaryResponse" to "java.util.Collection"
    )
)

openApiModule(
    module = "common",
    pkg = "com.klabis.common.users.infrastructure.restapi",
    apis = listOf("MyProfile", "PasswordSetup", "Permissions", "Root", "Dashboard"),
    models = listOf(
        "ChangePasswordRequest",
        "ValidateTokenResponse",
        "SetPasswordRequest",
        "PasswordSetupResponse",
        "TokenRequestRequest",
        "TokenRequestResponse",
        "UpdatePermissionsRequest"
    ),
    mappings = mapOf(
        "EntityModelPermissionsResponse" to "com.klabis.common.users.infrastructure.restapi.PermissionsResponse",
        "Authority" to "com.klabis.common.users.Authority",
        // rootNavigation/dashboard follow the standard "returning plain payloads" pattern like every
        // other migrated module: the generated interface returns the plain RootModel/DashboardModel
        // marker record, and the controller populates HalResponseContext.setDomain(...) with a
        // non-null placeholder so HalResponseBodyAdvice.wrapSingle() wraps it into
        // EntityModelWithDomain<RootModel, String> (which extends EntityModel<RootModel>) and runs it
        // through the postprocessor pipeline. The nine RepresentationModelProcessor<EntityModel<RootModel>>
        // beans (plus the DashboardModel one) are typed on the CONTENT (EntityModel<RootModel>), not
        // on the domain type parameter, so they still bind correctly. See RootController/
        // DashboardController for the actual setDomain(...) call and value chosen.
        "EntityModelRootModel" to "com.klabis.common.ui.RootModel",
        "EntityModelDashboardModel" to "com.klabis.common.ui.DashboardModel"
    )
)

openApiModule(
    module = "oris",
    pkg = "com.klabis.oris",
    // "OrisImport", not "ORIS": the generator's `apis` filter matches tags by substring, and
    // events.yaml already owns "OrisEvents" — "ORIS" alone matches both tags and pulls
    // OrisEventsApi's operations in here instead of/alongside this module's own. See oris.yaml
    // header comment.
    apis = listOf("OrisImport"),
    // No real model to generate: OrisEventSummary is mapped below onto the hand-written record
    // already on the controller. An empty `models` list is NOT safe here — like the `apis` global
    // property, the generator's "models" property generates every schema in the bundled document
    // when given an empty string, not none. A single nonexistent placeholder name keeps the
    // "models" property non-empty (so the "generate everything" branch never triggers) while
    // matching nothing, so only the interface (OrisImportApi) is produced.
    models = listOf("_NoGeneratedModelsForOris"),
    mappings = mapOf(
        "OrisEventSummary" to "com.klabis.oris.OrisController.OrisEventSummary"
    )
)
