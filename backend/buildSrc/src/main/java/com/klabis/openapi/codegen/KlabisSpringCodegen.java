package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.utils.ModelUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Klabis-specific fork of the stock {@code spring} OpenAPI generator.
 *
 * <p>Vendor fork, diff on upgrade: this class overrides specific protected extension points of
 * {@link SpringCodegen} to teach the generator Klabis's own HAL conventions (envelope schemas,
 * tag-scoped model discovery) natively, instead of compensating for their absence with Gradle-side
 * whitelists, {@code schemaMappings}, and post-process patches. See
 * {@code openspec/changes/custom-openapi-codegen/design.md} for the full rationale and the
 * shape-detection rules this class implements.
 */
public class KlabisSpringCodegen extends SpringCodegen {

    @Override
    public String getName() {
        return "klabis-spring";
    }

    /**
     * Splits return-type resolution into two independent questions — see design.md Decision 2:
     * <ul>
     *     <li><b>Payload type</b> — resolved from the response schema (stock first-content-entry
     *     selection, unwrapped via {@link HalEnvelopeDetector} when it is a HAL envelope). No
     *     type-resolution logic is duplicated here: the actual computation of
     *     {@code returnType}/{@code returnBaseType}/imports/discriminators is delegated to
     *     {@code super}, reusing all of the stock generator's existing machinery.</li>
     *     <li><b>Container</b> — {@code Page<T>} if and only if the operation declares
     *     {@code x-spring-paginated: true}, applied AFTER the {@code super} call and independent
     *     of whether an envelope was detected, so an operation serving only {@code application/json}
     *     still gets its pagination metadata.</li>
     * </ul>
     * Widened to {@code public} (legal on override) so it is directly unit-testable against
     * in-memory {@link Operation}/{@link ApiResponse} fixtures — see design.md's migration plan.
     */
    @Override
    public void handleMethodResponse(Operation operation,
                                      Map<String, Schema> schemas,
                                      CodegenOperation op,
                                      ApiResponse methodResponse,
                                      Map<String, String> schemaMappings) {
        Schema<?> responseSchema = resolveResponseSchema(methodResponse);
        // HalEnvelopeDetector inspects allOf/properties structure, which lives on the RESOLVED
        // schema, not on a {$ref: "..."} pointer — the response's content almost always names the
        // envelope by $ref (e.g. getFeeGroup's sole application/prs.hal-forms+json entry), so the
        // detector would see an empty wrapper object and never find the allOf it is looking for.
        Schema<?> resolvedForDetection = resolveIfRef(responseSchema, schemas);
        Optional<EnvelopeUnwrap> unwrap = resolvedForDetection == null
            ? Optional.empty()
            : HalEnvelopeDetector.detect(resolvedForDetection, schemas);

        ApiResponse resolved = unwrap
            .map(u -> withSchema(methodResponse, unwrappedResponseSchema(u)))
            .orElse(methodResponse);
        super.handleMethodResponse(operation, schemas, op, resolved, schemaMappings);

        // Pagination comes from the operation, not from the response representation — see
        // Decision 2. Applied whether or not an envelope was detected, so an operation serving
        // only application/json still gets Page<T>.
        if (isPaginated(operation)) {
            op.returnContainer = "Page";
            op.returnType = "org.springframework.data.domain.Page<" + op.returnBaseType + ">";
            op.isArray = false;
            op.imports.add("Page");
        }
    }

    /**
     * Drops imports naming a HAL envelope schema, which is never generated as a Java class.
     *
     * <p>{@code fromOperation()} adds an import for each response's {@code baseType} while
     * assembling the operation, using the raw (still-enveloped) schema and running before either
     * {@link #fromResponse} or {@link #handleMethodResponse} can unwrap it. The unwrapped type is
     * imported too, so the envelope import is left over — and referencing a class the generator
     * was never asked to produce makes the API interface fail to compile.
     *
     * <p>Runs last, once every operation and its imports are final, which is why it belongs here
     * rather than in the two unwrap hooks.
     */
    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationsMap processed = super.postProcessOperationsWithModels(objs, allModels);

        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        for (Iterator<Map<String, String>> it = processed.getImports().iterator(); it.hasNext(); ) {
            String imported = it.next().get("import");
            if (imported == null) {
                continue;
            }
            String simpleName = imported.substring(imported.lastIndexOf('.') + 1);
            Schema<?> schema = schemas.get(simpleName);
            if (schema != null && HalEnvelopeDetector.detect(schema, schemas).isPresent()) {
                it.remove();
            }
        }
        return processed;
    }

    /**
     * Applies the same envelope unwrap to each documented response as {@code
     * handleMethodResponse()} applies to the method's return type.
     *
     * <p>Needed because {@code fromOperation()} builds every {@code CodegenResponse} — which is
     * what {@code api.mustache} renders into {@code @ApiResponse(content = @Content(schema =
     * @Schema(implementation = ...)))} — from the raw response schema, and does so BEFORE
     * {@code handleMethodResponse()} runs. Without this override the method signature would
     * correctly say {@code MembershipFeeGroupResponse} while its {@code @Schema} still named the
     * envelope {@code EntityModelMembershipFeeGroupResponseWithMembers}, which is never generated
     * as a Java class — the interface would not compile. Previously the {@code --strip-hal}
     * pre-process and the {@code doLast} regex patch kept these two in step; resolving the
     * envelope here is what lets both go away.
     */
    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        Schema<?> responseSchema = resolveResponseSchema(response);
        Schema<?> resolvedForDetection = resolveIfRef(responseSchema, ModelUtils.getSchemas(openAPI));
        Optional<EnvelopeUnwrap> unwrap = resolvedForDetection == null
            ? Optional.empty()
            : HalEnvelopeDetector.detect(resolvedForDetection, ModelUtils.getSchemas(openAPI));

        if (unwrap.isEmpty()) {
            return super.fromResponse(responseCode, response);
        }
        return super.fromResponse(responseCode, withSchema(response, unwrappedResponseSchema(unwrap.get())));
    }

    /**
     * Mirrors the stock generator's own content selection (first entry in the response's
     * {@code content} map — see {@code ModelUtils.getSchemaFromContent}; the bundler already sorts
     * {@code application/json} first wherever it exists), used only to feed {@link
     * HalEnvelopeDetector}. The full {@code content} map is untouched and still drives the
     * {@code produces} clause, computed separately in {@code fromOperation()}'s
     * {@code addProducesInfo(...)} call before {@code handleMethodResponse()} ever runs.
     */
    private static Schema<?> resolveResponseSchema(ApiResponse response) {
        Content content = response.getContent();
        if (content == null || content.isEmpty()) {
            return null;
        }
        MediaType first = content.values().iterator().next();
        return first == null ? null : first.getSchema();
    }

    /**
     * The schema {@code handleMethodResponse()} should see for an unwrapped envelope: the bare
     * payload type for Shape 1, or an {@code array} of it for Shape 2 — {@code
     * ModelUtils.isArraySchema(...)} (in the {@code super} call this feeds) is what makes the
     * stock generator set {@code cm.containerType = "list"}/{@code op.isArray = true}, so a
     * collection unwrap target must be wrapped back into an array shape, not passed bare.
     */
    private static Schema<?> unwrappedResponseSchema(EnvelopeUnwrap unwrap) {
        if (!unwrap.isCollection()) {
            return unwrap.targetSchema();
        }
        return new Schema<>().type("array").items(unwrap.targetSchema());
    }

    /**
     * Returns a shallow copy of {@code response} whose {@code content} contains a single entry
     * wrapping {@code targetSchema} — {@code handleMethodResponse()} in {@code DefaultCodegen}
     * re-derives the schema itself from the response's content map (it takes no schema parameter),
     * so the only way to feed it an already-unwrapped payload is to rewrite the response passed in.
     */
    private static ApiResponse withSchema(ApiResponse response, Schema<?> targetSchema) {
        ApiResponse rewritten = new ApiResponse();
        rewritten.setDescription(response.getDescription());
        rewritten.setHeaders(response.getHeaders());
        rewritten.setContent(new Content().addMediaType("application/json", new MediaType().schema(targetSchema)));
        return rewritten;
    }

    private static Schema<?> resolveIfRef(Schema<?> schema, Map<String, Schema> schemas) {
        if (schema == null || schema.get$ref() == null) {
            return schema;
        }
        String ref = schema.get$ref();
        String name = ref.substring(ref.lastIndexOf('/') + 1);
        return schemas.get(name);
    }

    private static boolean isPaginated(Operation operation) {
        if (operation.getExtensions() == null) {
            return false;
        }
        Object value = operation.getExtensions().get("x-spring-paginated");
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
