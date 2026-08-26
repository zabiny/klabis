import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

/**
 * Registers a spec-first codegen task for one module and wires its output into the main sourceSet.
 *
 * One generate task PER MODULE: modelPackage/apiPackage are scalars, and schemaMappings is global
 * per task — two modules each defining their own (say) AddressRequest would collide irreconcilably.
 * Per-module tasks keep each module's mappings in its own namespace, which is what makes this scale
 * past the two pilot modules.
 *
 * Each module generates into the same package as its hand-written controller/mapper, because
 * cross-module link processors depend on those packages via Modulith named interfaces.
 *
 * Generates directly from the module's own `docs/openapi/spec/<specFile>` — each spec file's full
 * `paths`/`components.schemas` content *is* the module's generation scope now, one file per module.
 * `openapiBundle`'s merged `docs/openapi/klabis-full.json` still exists for the frontend's
 * TypeScript codegen and Swagger UI/Redoc, but backend codegen no longer reads it.
 *
 * @param module   short module name; also the build/generated/openapi/<module> output directory
 * @param pkg      target package for both models and APIs
 * @param specFile path under docs/openapi/spec/ this module generates from (its full paths/schemas
 *                 scope, no models/apis enumeration needed)
 * @param mappings schema name to existing Java type, for types reused instead of regenerated. HAL
 *                 envelope schemas no longer need an entry here — KlabisSpringCodegen unwraps them
 *                 structurally, see custom-openapi-codegen design.md. Only hand-written overrides
 *                 the generator cannot infer from spec structure alone still need one: nested
 *                 classes, domain-enum redirection, cross-module application types, and marker
 *                 types shaped as a plain object with only a links property (no allOf, no embedded
 *                 block — deliberately not matched by HalEnvelopeDetector).
 */
fun Project.openApiModule(
    module: String,
    pkg: String,
    specFile: String,
    mappings: Map<String, String>,
    extraImportMappings: Map<String, String> = emptyMap()
) {
    val openapiBundle = tasks.named("openapiBundle")
    val outputDir = layout.buildDirectory.dir("generated/openapi/$module")

    val task = tasks.register<GenerateTask>(
        "openApiGenerate${module.replaceFirstChar { it.uppercase() }}"
    ) {
        group = "openapi"
        description = "Generates spec-first DTOs and API interfaces for the $module module"
        dependsOn(openapiBundle)

        // The generator only writes files; it never removes ones it no longer produces. Without
        // this, a schema renamed or dropped in the spec leaves its old record behind in build/ —
        // and since the directory is on the main sourceSet, that ghost still compiles. Local builds
        // then keep working against a type the spec no longer defines, and only a clean CI build
        // fails.
        doFirst { project.delete(outputDir) }

        // com.klabis.openapi.codegen.KlabisSpringCodegen, registered in buildSrc via
        // META-INF/services. Resolves HAL envelopes structurally — see its class Javadoc.
        generatorName.set("klabis-spring")
        // Reads the module's own hand-written spec file directly — openapi-generator resolves
        // relative cross-file $refs (e.g. ./_shared/hal.yaml) the same way bundle.mjs does, so no
        // pre-bundled document is needed for backend codegen. dependsOn(openapiBundle) above is kept
        // anyway for its validateSpec/validateModuleDocuments side effect (see design.md Decision 5)
        // — a domain file drifted from klabis.yaml still fails the build here, not silently.
        inputSpec.set(layout.projectDirectory.file("../docs/openapi/spec/$specFile").asFile.absolutePath)
        this.outputDir.set(outputDir.map { it.asFile.absolutePath })
        templateDir.set(layout.projectDirectory.dir("src/main/openapi-templates").asFile.absolutePath)
        modelPackage.set(pkg)
        apiPackage.set(pkg)
        skipValidateSpec.set(true)

        globalProperties.set(
            mapOf(
                // Empty string, NOT an omitted key: the generator's "models"/"apis" GlobalSettings
                // properties mean "generate all" only when present-but-empty. An omitted key skips
                // generation entirely (verified via --info: "Skipping generation of models/APIs.").
                "models" to "",
                "apis" to "",
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
                "documentationProvider" to "springdoc",
                // Emits JsonNullable<T> for a nullable property, giving PATCH bodies their
                // absent/null/value tri-state. Requires the OpenAPI 3.1 spelling
                // `type: [x, "null"]` — the 3.0 `nullable: true` keyword leaves isNullable false.
                "openApiNullable" to "true",
                "useTags" to "true",
                "additionalModelTypeAnnotations" to
                    "@io.soabase.recordbuilder.core.RecordBuilder @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @org.springframework.security.authorization.method.HandleAuthorizationDenied(handlerClass = com.klabis.common.security.fieldsecurity.NullDeniedHandler.class)"
            )
        )

        // date-time wire format maps to Instant (not the generator's default OffsetDateTime) to match
        // the rest of the codebase (see backend-patterns: ZonedDateTime/Instant in domain, LocalDate in API).
        typeMappings.set(mapOf("DateTime" to "Instant"))

        // Envelope schemas (EntityModel*/PagedModel*/CollectionModel*) are auto-unwrapped structurally
        // by KlabisSpringCodegen (see custom-openapi-codegen design.md) — only hand-written overrides
        // the generator cannot infer from spec structure alone (nested classes, domain-enum
        // redirection, cross-module application types, marker types with no allOf/_embedded shape)
        // need a mappings entry here. The API interface signature must match what HalResponseBodyAdvice
        // expects (plain payload DTO / Page<T> / List<T>, see ADR-002). KlabisSpringCodegen also never
        // emits an illegal generic class literal (e.g. `Page<X>.class`) in the springdoc `@Schema`
        // annotations it generates, so no post-process patch is needed for that either (see design.md
        // Decision 4).
        val commonMappings = mapOf("ProblemDetail" to "org.springframework.http.ProblemDetail")
        schemaMappings.set(mappings + commonMappings)
        importMappings.set(mappings + commonMappings + extraImportMappings + mapOf("Instant" to "java.time.Instant"))
    }

    // Part of the main sourceSet (not a separate one) so Lombok -> MapStruct -> RecordBuilder
    // annotation processors run over the generated records exactly as they do for hand-written code.
    the<SourceSetContainer>().named("main") { java.srcDir(outputDir.map { it.dir("src/main/java") }) }
    tasks.named("compileJava") { dependsOn(task) }
}
