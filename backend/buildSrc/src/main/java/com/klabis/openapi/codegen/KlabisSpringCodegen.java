package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.openapitools.codegen.CodegenMediaType;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.utils.ModelUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Klabis-specific fork of the stock {@code spring} OpenAPI generator.
 *
 * <p>Vendor fork, diff on upgrade: this class overrides specific protected/public extension points
 * of {@link SpringCodegen} to teach the generator Klabis's own HAL envelope conventions natively,
 * instead of compensating for their absence with Gradle-side {@code schemaMappings}, a
 * {@code --strip-hal} pre-process bundle, and a {@code doLast} regex post-process — all three now
 * removed. See {@code openspec/changes/custom-openapi-codegen/design.md} for the full rationale and
 * the shape-detection rules {@link HalEnvelopeDetector} implements.
 *
 * <p>Every override here exists to keep one property true: the method signature and its Javadoc/doc
 * comment always agree about the return type.
 * <ul>
 *     <li>{@link #handleMethodResponse} — resolves {@code op.returnType} (the method signature) by
 *     unwrapping a detected envelope and applying {@code Page<T>} for a paginated operation.</li>
 *     <li>{@link #fromResponse} — resolves each documented {@code CodegenResponse} (the
 *     {@code @Schema(implementation = ...)} springdoc emits) the same way, and additionally
 *     suppresses that doc block entirely wherever the true type is not a legal Java class literal
 *     ({@code Page<T>}, or a {@code schemaMappings} target of {@code java.lang.Object}).</li>
 *     <li>{@link #fromOperation} — records the current {@link Operation} in an instance field so
 *     {@link #fromResponse} (which the stock signature gives no operation context) can still read
 *     {@code x-spring-paginated} off it.</li>
 *     <li>{@link #getContent} — prevents an {@code import} of an envelope class from ever being
 *     added, by unwrapping each media type's schema before {@code super} walks the content map; a
 *     class the generator never produces would otherwise fail the build.</li>
 * </ul>
 * Discovering which schemas/tags to generate at all ({@code models}/{@code apis} allow-lists) is
 * NOT something this class controls — that lives entirely in {@code DefaultGenerator}, outside any
 * {@code CodegenConfig} hook (design.md Decision 5, withdrawn). Per-module {@code models} lists in
 * {@code build.gradle.kts} stay required.
 */
public class KlabisSpringCodegen extends SpringCodegen {

    /**
     * Set by {@link #fromOperation} before delegating to {@code super}, cleared in a
     * {@code finally} block; consulted by {@link #fromResponse} to know whether the response it
     * is currently building belongs to a paginated operation. {@code fromResponse(String,
     * ApiResponse)}'s signature (inherited, cannot be widened) carries no operation reference, and
     * {@code DefaultCodegen.fromOperation()} calls it once per response code (200, 400, 401, ...)
     * from inside its own loop — an instance field set around that one call is the only way to
     * thread the signal through without duplicating the whole loop here.
     */
    private Operation currentOperation;

    public KlabisSpringCodegen() {
        // "Page" is the simple name handleMethodResponse() adds to op.imports when an operation is
        // paginated (see the override below). DefaultGenerator resolves a simple import name via
        // importMapping() first, falling back to modelPackage + "." + name when absent — without
        // this entry the generated import silently named a nonexistent class in the module's own
        // restapi package instead of Spring Data's Page, which failed to compile. Same registration
        // point SpringCodegen itself uses for "Pageable" (see SpringCodegen.processOpts()).
        importMapping.put("Page", "org.springframework.data.domain.Page");
    }

    @Override
    public String getName() {
        return "klabis-spring";
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        currentOperation = operation;
        try {
            return super.fromOperation(path, httpMethod, operation, servers);
        } finally {
            currentOperation = null;
        }
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
        Optional<EnvelopeUnwrap> unwrap = detectUnwrap(responseSchema, schemas);

        ApiResponse resolved = unwrap
            .map(u -> withSchema(methodResponse, unwrappedResponseSchema(u)))
            .orElse(methodResponse);
        super.handleMethodResponse(operation, schemas, op, resolved, schemaMappings);

        // Pagination comes from the operation, not from the response representation — see
        // Decision 2. Applied whether or not an envelope was detected, so an operation serving
        // only application/json still gets Page<T>.
        if (isPaginated(operation)) {
            // returnContainer stays UNSET (not "Page"): JavaSpring/returnTypes.mustache only
            // renders `{{{returnContainer}}}<{{{returnType}}}>` inside the `{{#isArray}}` branch.
            // For this non-array response it must fall through `{{^returnContainer}}` instead,
            // which renders `{{{returnType}}}` verbatim — so returnType already carries the full
            // `Page<X>` string rather than relying on returnContainer to wrap it.
            op.returnType = "org.springframework.data.domain.Page<" + op.returnBaseType + ">";
            op.returnContainer = null;
            op.isArray = false;
            op.imports.add("Page");
        }
    }

    /**
     * Prevents an import of a HAL envelope schema from ever being added, by mapping each media
     * type's schema through the same unwrap pipeline as {@link #fromResponse} and {@link
     * #handleMethodResponse} before delegating to {@code super}.
     *
     * <p>{@code DefaultCodegen.fromOperation()} calls this directly on the response's raw content
     * map (sources line ~4707) BEFORE either of those two overrides ever runs, adding an import
     * for every media type's schema it walks — including the envelope, which is never generated
     * as a Java class and would otherwise fail the build. Widened to {@code public} (legal on
     * override) for direct unit testing, matching {@link #handleMethodResponse}.
     */
    @Override
    public LinkedHashMap<String, CodegenMediaType> getContent(Content content, Set<String> imports, String mediaTypeSchemaSuffix) {
        if (content == null) {
            return super.getContent(null, imports, mediaTypeSchemaSuffix);
        }
        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        Content unwrapped = new Content();
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            MediaType mediaType = entry.getValue();
            Schema<?> schema = mediaType == null ? null : mediaType.getSchema();
            Optional<EnvelopeUnwrap> unwrap = detectUnwrap(schema, schemas);

            MediaType rewritten = unwrap
                .map(u -> new MediaType().schema(unwrappedResponseSchema(u)))
                .orElse(mediaType);
            unwrapped.addMediaType(entry.getKey(), rewritten);
        }
        return super.getContent(unwrapped, imports, mediaTypeSchemaSuffix);
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
        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        Schema<?> resolvedForDetection = HalEnvelopeDetector.resolveRef(resolveResponseSchema(response), schemas);
        Optional<EnvelopeUnwrap> unwrap = resolvedForDetection == null
            ? Optional.empty()
            : HalEnvelopeDetector.detect(resolvedForDetection, schemas);

        ApiResponse effective = unwrap
            .map(u -> withSchema(response, unwrappedResponseSchema(u)))
            .orElse(response);
        CodegenResponse r = super.fromResponse(responseCode, effective);

        // A collection response — whether a Shape 2 envelope OR a bare array schema (e.g.
        // listMembers' application/json sibling MemberSummaryResponseList, which
        // resolveResponseSchema picks first per the bundler's sort order and which does NOT match
        // HalEnvelopeDetector at all) — resolves to Page<T> on a paginated operation, per
        // Decision 2: pagination is a property of the operation, independent of which response
        // representation is used.
        boolean paginatedCollection = currentOperation != null
            && isPaginated(currentOperation)
            && unwrap.map(EnvelopeUnwrap::isCollection)
                .orElseGet(() -> resolvedForDetection != null && ModelUtils.isArraySchema(resolvedForDetection));

        return suppressUnrenderableDocBlock(r, paginatedCollection);
    }

    /**
     * Clears {@code baseType} whenever the response's true type cannot be written as a Java class
     * literal, which is what {@code @Schema(implementation = ...)} requires (JLS 15.8.2,
     * Decision 4). Two cases reach this today, and they are one rule rather than two:
     * <ul>
     *     <li>a paginated collection, whose true type is the generic {@code Page<T>};</li>
     *     <li>a {@code schemaMappings} target of {@code java.lang.Object} (e.g.
     *     {@code SuspensionBlockedWarning}, a discriminator-less {@code oneOf} no single Java type
     *     stands for) — springdoc renders {@code Object.class} as {@code "type": "string"}, which
     *     is worse than omitting the block.</li>
     * </ul>
     * Leaving {@code baseType} unset means {@code api.mustache}'s
     * {@code &#123;&#123;#baseType&#125;&#125;} section around the whole {@code content = &#123;...&#125;}
     * block never opens, so no illegal literal is emitted in the first place — which is what the
     * old {@code doLast} regex patch used to remove after the fact.
     */
    private static CodegenResponse suppressUnrenderableDocBlock(CodegenResponse r, boolean paginatedCollection) {
        if (paginatedCollection || "Object".equals(r.baseType) || "java.lang.Object".equals(r.baseType)) {
            r.baseType = null;
        }
        return r;
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
     * Resolves {@code schema} through any {@code $ref} and runs {@link HalEnvelopeDetector} on the
     * result — the detector inspects allOf/properties structure, which lives on the resolved
     * schema, not on a {@code {$ref: "..."}} pointer.
     */
    private static Optional<EnvelopeUnwrap> detectUnwrap(Schema<?> schema, Map<String, Schema> schemas) {
        Schema<?> resolved = HalEnvelopeDetector.resolveRef(schema, schemas);
        return resolved == null ? Optional.empty() : HalEnvelopeDetector.detect(resolved, schemas);
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
