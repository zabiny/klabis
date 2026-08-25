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
// Generates directly from docs/openapi/klabis-full.json — the same bundle openapiBundle produces
// for the frontend. KlabisSpringCodegen resolves HAL envelope schemas to their payload type
// structurally (see custom-openapi-codegen design.md), so no separate codegen-only bundle with HAL
// content blanked out is needed anymore.
// ---------------------------------------------------------------------------

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
        "SuspendMembershipRequest",
        "UpdateMemberRequest",
        // Enum schemas — must be listed explicitly so the generator emits them; without a
        // schemaMapping redirecting them onto the domain enum, they need `models` to know to
        // generate the type at all (see the mapping comment below for why they're no longer mapped).
        "Gender",
        "UpdateMemberRequest_gender",
        "DeactivationReason",
        "DrivingLicenseGroup",
        "TrainerLicenseDto_level",
        "RefereeLicenseDto_level"
    ),
    mappings = mapOf(
        // Gender/DeactivationReason/DrivingLicenseGroup/TrainerLicenseDto_level/RefereeLicenseDto_level
        // used to be redirected here onto the hand-written domain enums (Gender, DeactivationReason,
        // DrivingLicenseGroup, TrainerLevel, RefereeLevel). Now that model.mustache renders a real
        // enumOuterClass for a promoted/$ref'd top-level enum, the generator synthesizes its own DTO
        // enum for each of these instead — see MemberMapper/UpdateMemberRequestMapper for the explicit
        // conversion between the generated DTO enum and the domain enum at the REST boundary.
        // listMembers' PagedModelEntityModelMemberSummaryResponse (Shape 2) and its application/json
        // sibling MemberSummaryResponseList are now both resolved to Page<MemberSummaryResponse> by
        // KlabisSpringCodegen from x-spring-paginated, with no mappings entry needed — see
        // custom-openapi-codegen design.md Decision 2.
        // suspendMember's 409 body: a oneOf of two records that MembersExceptionHandler declares
        // and the interface never names (it returns ResponseEntity<Void>), so no Java type stands
        // for the union. Object is the honest answer, and it also stops the generator importing a
        // model it was not asked to generate. Generating the union instead does not work — the
        // generator flattens a discriminator-less oneOf into one record holding every branch's
        // fields, each @NotNull, which nothing can satisfy. The published contract in
        // klabis-full.json still carries the full oneOf; only /v3/api-docs goes without, since
        // KlabisSpringCodegen suppresses the doc-block content for a java.lang.Object mapping
        // (springdoc would otherwise infer `"type": "string"`, which is worse than saying nothing).
        "SuspensionBlockedWarning" to "java.lang.Object"
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
    mappings = mapOf()
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
    // listTransactions' PagedModelEntityModelTransactionResource (Shape 2 HAL envelope) and its
    // application/json sibling TransactionResourcePage (a named array schema) are now both resolved
    // to Page<TransactionResource> by KlabisSpringCodegen from x-spring-paginated, with no mappings
    // entry needed — see custom-openapi-codegen design.md Decision 2.
    mappings = mapOf()
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
        "UpdateCategoryPresetRequest",
        "UpdateEventRequest",
        "UpdateEventCategoryRequest",
        "UpdateEventRankingRequest",
        "EntryFeeRequest",
        "CreateEventRequest",
        "CreateEventCategoryRequest",
        // Enum schema — must be listed explicitly so the generator emits it (see the mapping comment
        // below for why it's no longer redirected onto the domain enum).
        "EventStatus"
    ),
    mappings = mapOf(
        // EventStatus used to be redirected here onto the hand-written domain enum
        // com.klabis.events.domain.EventStatus. Now that model.mustache renders a real enumOuterClass
        // for a top-level enum schema, the generator synthesizes its own DTO enum instead — see
        // EventDto/EventSummaryDto/EventController for the explicit conversion to/from the domain enum.
        // listEvents' PagedModelEntityModelEventSummaryDto (Shape 2 HAL envelope) and its
        // application/json sibling EventSummaryDtoList (a named array schema) are now both resolved
        // to Page<EventSummaryDto> by KlabisSpringCodegen from x-spring-paginated, with no mappings
        // entry needed — see custom-openapi-codegen design.md Decision 2.
        // getEvent returns the payload; the _embedded.registrationDtoList block in the
        // WithRegistrations schema is contributed by the controller via HalResponseContext.embed(...)
        // and assembled by HalResponseBodyAdvice, so it does not belong in the Java return type.
        // This is a hand-written override the generator cannot infer from spec structure alone
        // (it stays a Shape 1 envelope by shape, but the target Java class carries no _embedded
        // block) — schema name differs from the target Java class (EventDto), so this mapping stays.
        "EntityModelEventDtoWithRegistrations" to "com.klabis.events.infrastructure.restapi.EventDto",
        // BulkSyncResult/BulkImportResult are application-layer types, not restapi DTOs — the
        // envelope and application/json-sibling schema names never match the target package, so both
        // stay mapped (otherwise springdoc's @Schema(implementation = ...) resolves against pkg,
        // where no such class exists).
        "EntityModelBulkSyncResult" to "com.klabis.events.application.BulkSyncResult",
        "EntityModelBulkImportResult" to "com.klabis.events.application.BulkImportResult",
        "BulkSyncResult" to "com.klabis.events.application.BulkSyncResult",
        "BulkImportResult" to "com.klabis.events.application.BulkImportResult"
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
    // No mappings: listCalendarItems' CalendarItemDtoList is a named array schema -> List<CalendarItemDto>
    // directly, and getCalendarItem/getTokenState/generateToken's application/json siblings already
    // match their target Java class names — nothing to redirect. No pagination in this module.
    mappings = mapOf()
)

openApiModule(
    module = "membershipfees",
    pkg = "com.klabis.membershipfees.infrastructure.restapi",
    // getFeeGroup (MembershipFeeGroups) is generated like any other operation; it is only its
    // *response schema* that is documentation-only. EntityModelMembershipFeeGroupResponseWithMembers
    // is a Shape 1 HAL envelope (allOf[$ref MembershipFeeGroupResponse, {_links, _embedded}]),
    // auto-unwrapped by KlabisSpringCodegen/HalEnvelopeDetector — see custom-openapi-codegen — so the
    // generated method does not try to express the second, independently-shaped collection (group
    // members) the controller embeds via HalModelBuilder. HalResponseContext only supports a single
    // domain object or a flat list. Same precedent as EventController's getEvent in the events module.
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
        // PaymentRuleResponse is a nested class (MembershipFeeTierResponse.PaymentRuleResponse); its
        // application/json bare-payload sibling (schema name PaymentRuleResponse, for getRule) would
        // otherwise resolve to a top-level PaymentRuleResponse class rather than the nested one, so
        // this mapping stays — it is a hand-written override the generator cannot infer from spec
        // structure alone, unlike the envelope unwrap above.
        "PaymentRuleResponse" to "com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierResponse.PaymentRuleResponse"
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
        // getFamilyGroup has no application/json sibling (its response's per-item _links can't be
        // expressed in a bare payload — see groups.yaml header comment), so backend codegen still
        // sees the full envelope schema. EntityModelFamilyGroupResponse's name differs from the
        // target Java class, so this mapping stays — same precedent as EntityModelEventDtoWithRegistrations
        // in the events module. Verified empirically: removing it degrades getFamilyGroup's return
        // type to the ungenerated envelope, breaking the controller's @Override.
        "EntityModelFamilyGroupResponse" to "com.klabis.groups.familygroup.infrastructure.restapi.FamilyGroupResponse"
        // listFamilyGroups' CollectionModelEntityModelFamilyGroupSummaryResponse envelope is now
        // auto-unwrapped by KlabisSpringCodegen from its array/_embedded shape (Shape 2, non-paginated
        // -> List<T>) — no mapping needed, matching the FamilyGroupSummaryResponseList sibling it used
        // to be stripped down to.
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
        // getGroup has no application/json sibling (per-item _links on owners/members/
        // pendingInvitations can't be expressed in a bare payload — see groups.yaml header comment),
        // so backend codegen still sees the full envelope schema. EntityModelGroupResponse's name
        // differs from the target Java class, so this mapping stays.
        "EntityModelGroupResponse" to "com.klabis.groups.freegroup.infrastructure.restapi.GroupResponse"
        // listGroups' and getPendingInvitations' envelopes are now auto-unwrapped by KlabisSpringCodegen
        // from their array/_embedded shape (Shape 2, non-paginated -> List<T>) — no mapping needed,
        // matching the GroupSummaryResponseList/PendingInvitationResponseList siblings they used to be
        // stripped down to.
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
        "AgeRangeRequest",
        "UpdateTrainingGroupRequest"
    ),
    mappings = mapOf(
        // getTrainingGroup has no application/json sibling (per-item _links on trainers/members
        // can't be expressed in a bare payload — see groups.yaml header comment), so backend codegen
        // still sees the full envelope schema. EntityModelTrainingGroupResponse's name differs from
        // the target Java class, so this mapping stays.
        "EntityModelTrainingGroupResponse" to "com.klabis.groups.traininggroup.infrastructure.restapi.TrainingGroupResponse"
        // listTrainingGroups' envelope is now auto-unwrapped by KlabisSpringCodegen from its
        // array/_embedded shape (Shape 2, non-paginated -> List<T>) — no mapping needed, matching the
        // TrainingGroupSummaryResponseList sibling it used to be stripped down to.
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
        // PermissionsResponse is hand-written (not in `models`, so never regenerated). The
        // application/json sibling on getUserPermissions references it directly by its own schema
        // name, which already matches the target Java class — so only this bare mapping is needed;
        // the envelope schema EntityModelPermissionsResponse never needs its own mapping.
        "PermissionsResponse" to "com.klabis.common.users.infrastructure.restapi.PermissionsResponse",
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
