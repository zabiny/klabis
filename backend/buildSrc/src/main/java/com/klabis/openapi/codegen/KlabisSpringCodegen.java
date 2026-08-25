package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.languages.SpringCodegen;

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
        Optional<EnvelopeUnwrap> unwrap = responseSchema == null
            ? Optional.empty()
            : HalEnvelopeDetector.detect(responseSchema, schemas);

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

    private static boolean isPaginated(Operation operation) {
        if (operation.getExtensions() == null) {
            return false;
        }
        Object value = operation.getExtensions().get("x-spring-paginated");
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
