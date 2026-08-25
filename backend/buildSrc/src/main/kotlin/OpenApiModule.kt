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
 * Generates directly from docs/openapi/klabis-full.json — the same bundle openapiBundle produces
 * for the frontend. KlabisSpringCodegen resolves HAL envelope schemas to their payload type
 * structurally (see custom-openapi-codegen design.md), so no separate codegen-only bundle with HAL
 * content blanked out is needed anymore.
 *
 * @param module   short module name; also the build/generated/openapi/<module> output directory
 * @param pkg      target package for both models and APIs
 * @param apis     OpenAPI tags to generate interfaces for. Must be listed explicitly: the "apis"
 *                 global property generates EVERY tag when left empty, so without this filter each
 *                 module's task would emit every other module's *Api.java into its own package.
 * @param models   payload schemas to generate as Java records (envelopes stay spec-only)
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
    apis: List<String>,
    models: List<String>,
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
        // The committed frontend-facing bundle — openapiBundle's default (no -PopenapiOut/-PopenapiCheck)
        // writes exactly here, so a plain `dependsOn(openapiBundle)` is enough to keep this fresh.
        // Codegen and the frontend read the same document now; the separate --strip-hal bundle is
        // gone, since KlabisSpringCodegen resolves HAL envelopes structurally instead.
        //
        // Caveat: -PopenapiCheck makes openapiBundle validate without writing, so pairing it with a
        // build task leaves codegen reading whatever is committed rather than a freshly bundled
        // spec. Harmless as things stand — CI keeps the two apart (publish-api.yaml bundles via
        // node directly, backend-tests.yml runs plain `./gradlew test`) — but do not pair
        // -PopenapiCheck with compileJava expecting a regeneration.
        inputSpec.set(layout.projectDirectory.file("../docs/openapi/klabis-full.json").asFile.absolutePath)
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
