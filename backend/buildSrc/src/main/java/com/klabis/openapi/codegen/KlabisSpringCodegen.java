package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
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
 *     <li>{@link #postProcessOperationsWithModels} — drops any leftover {@code import} of an
 *     envelope class once every operation in a tag is final; a class the generator never produces
 *     would otherwise fail the build.</li>
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

        boolean paginated = currentOperation != null && isPaginated(currentOperation);
        // A collection response (whether a Shape 2 envelope OR a bare array schema — e.g.
        // listMembers' application/json sibling MemberSummaryResponseList, which resolveResponseSchema
        // picks first per the bundler's sort order and which does NOT match HalEnvelopeDetector at
        // all) resolves to Page<T> on a paginated operation, per Decision 2 — pagination is a
        // property of the operation, independent of which response representation is used. Page<T>
        // is a generic type, not legal in @Schema(implementation = ...) (JLS 15.8.2, Decision 4).
        // Rather than generating an illegal class literal and patching it back out (the old doLast
        // regex, now removed), suppress the doc content block entirely by leaving CodegenResponse's
        // baseType unset: api.mustache's `{{#baseType}}...{{/baseType}}` around the whole
        // `content = {...}` block never opens. This mirrors what the prior schemaMappings ->
        // Page<X> pipeline produced (no content block for this response at all).
        boolean isCollectionShape = unwrap.map(EnvelopeUnwrap::isCollection)
            .orElseGet(() -> resolvedForDetection != null && ModelUtils.isArraySchema(resolvedForDetection));
        if (paginated && isCollectionShape) {
            CodegenResponse r = unwrap.isPresent()
                ? super.fromResponse(responseCode, withSchema(response, unwrappedResponseSchema(unwrap.get())))
                : super.fromResponse(responseCode, response);
            r.baseType = null;
            return r;
        }

        CodegenResponse r = unwrap.isEmpty()
            ? super.fromResponse(responseCode, response)
            : super.fromResponse(responseCode, withSchema(response, unwrappedResponseSchema(unwrap.get())));
        return suppressUnusableObjectDocBlock(r);
    }

    /**
     * A schemaMappings target of {@code java.lang.Object} (e.g. {@code SuspensionBlockedWarning} —
     * a discriminator-less {@code oneOf} no single Java type stands for) is not legal in
     * {@code @Schema(implementation = ...)} either, for the same reason as a generic {@code Page<X>}
     * class literal is not (Decision 4): springdoc renders {@code Object.class} as
     * {@code "type": "string"}, which is worse than omitting the block. Suppressing it here — by
     * leaving {@code baseType} unset, the same mechanism the paginated-response branch above uses —
     * replaces what the old {@code doLast} regex patch did for this case.
     */
    private static CodegenResponse suppressUnusableObjectDocBlock(CodegenResponse r) {
        if ("Object".equals(r.baseType) || "java.lang.Object".equals(r.baseType)) {
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
