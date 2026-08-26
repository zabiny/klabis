package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.openapitools.codegen.CodegenMediaType;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 *     <li>{@link #fromProperty} — Category C (design.md Decision 3): unwraps a model property whose
 *     items are a Shape 1-item envelope ({@code array of $ref allOf[payload, {_links,...}]}) to a
 *     bare {@code List<Payload>}, never {@code List<EntityModel<Payload>>} — the same rewrite
 *     principle as {@link #handleMethodResponse}, just applied to a property's schema instead of an
 *     operation's response schema.</li>
 *     <li>{@link #postProcessAllModels} — with the per-module {@code models}/{@code apis}
 *     allow-lists dropped ({@code globalProperties["models"|"apis"] = ""}, DefaultGenerator's own
 *     "generate everything in the document" sentinel), every HAL envelope schema and every schema
 *     that exists purely as one of its decomposition fragments (promoted inline {@code allOf}/
 *     {@code _embedded} sub-schemas) would otherwise be generated as a real, unreferenced
 *     {@code .java} file — these are never real Java types, the whole point of
 *     {@link HalEnvelopeDetector} is that they are structurally unwrapped away everywhere else in
 *     this class. This override removes both from the model map before templates render.</li>
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
     * Category C (design.md Decision 3) — rewrites a model property shaped as
     * {@code array of $ref to allOf[payload, {_links,...}]} (Shape 1-item, e.g.
     * {@code FamilyGroupResponse.parents}/{@code GroupResponse.owners}) to a bare
     * {@code array of $ref-to-payload} schema before delegating to {@code super}, so the resolved
     * Java type is {@code List<Payload>} — never {@code List<EntityModel<Payload>>}. HAL
     * {@code _links} per item stays a runtime concern of the existing
     * {@code RepresentationModelProcessor}/{@code EntityModel.of(...)} call sites, untouched by this
     * rewrite.
     *
     * <p><b>Exception — an explicit {@code schemaMappings} entry wins.</b> When the array item's
     * {@code $ref} names a schema that is itself a key in {@code schemaMapping()}, this override
     * steps aside entirely and delegates the ORIGINAL (unrewritten) property to {@code super}. This
     * is what lets a module redirect a specific {@code EntityModelX} envelope schema onto
     * {@code org.springframework.hateoas.EntityModel<X>} (a real generic Java type, via
     * {@code schemaMappings}/{@code importMapping} — see {@code build.gradle.kts}'s
     * {@code groupsFamily}/{@code groupsFree} modules) instead of this override's default
     * strip-to-bare-payload behavior. Without this guard, this override would rewrite the property
     * to {@code array of $ref Payload} BEFORE {@code super.fromProperty} ever sees the original
     * {@code $ref}, so the {@code schemaMapping} entry keyed on the envelope schema name would never
     * be consulted — confirmed empirically: the two mechanisms are mutually exclusive per schema,
     * and an explicit {@code schemaMappings} entry is a deliberate, spec-adjacent opt-in that must
     * take precedence over this override's own structural default.
     *
     * <p>Mirrors {@link #handleMethodResponse}'s rewrite-then-delegate shape otherwise: this method
     * never duplicates {@code DefaultCodegen}'s own property-resolution logic, it only substitutes
     * the raw {@link Schema} {@code super} sees. {@link HalEnvelopeDetector#detectPropertyItemUnwrap}
     * is the pure structural check (already unit-tested against the
     * {@code EntityModelTrainerResponse}-style fixture in {@code HalEnvelopeDetectorPropertyItemTest})
     * — this override supplies the missing per-model call site {@code HalEnvelopeDetector} alone
     * cannot reach.
     */
    @Override
    public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean schemaIsFromAdditionalProperties) {
        if (isMappedEnvelopeItem(p)) {
            return super.fromProperty(name, p, required, schemaIsFromAdditionalProperties);
        }

        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        Optional<EnvelopeUnwrap> unwrap = HalEnvelopeDetector.detectPropertyItemUnwrap(p, schemas);

        Schema<?> effective = unwrap
            .map(u -> (Schema<?>) new Schema<>().type("array").items(u.targetSchema()))
            .orElse(p);
        return super.fromProperty(name, effective, required, schemaIsFromAdditionalProperties);
    }

    /**
     * True when {@code p} is an array whose items are a {@code $ref} naming a schema explicitly
     * present in {@code schemaMapping()} — the signal that a module opted this specific envelope
     * schema out of Category C's default strip-to-bare-payload behavior in favor of an explicit
     * generic-type redirect (e.g. onto {@code EntityModel<X>}). Deliberately does not resolve or
     * inspect the referenced schema's shape at all: presence in {@code schemaMapping()} is by itself
     * a stronger, more explicit signal than the structural Shape 1-item check.
     */
    private boolean isMappedEnvelopeItem(Schema<?> p) {
        if (p == null || !ModelUtils.isArraySchema(p)) {
            return false;
        }
        Schema<?> items = p.getItems();
        if (items == null || items.get$ref() == null) {
            return false;
        }
        String schemaName = items.get$ref().substring(items.get$ref().lastIndexOf('/') + 1);
        return schemaMapping().containsKey(schemaName);
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


    /**
     * Removes every HAL envelope schema, and every model that exists only as a decomposition
     * fragment of one, from the model map before templates render — see the class Javadoc entry
     * above and {@code openspec/changes/openapi-spec-first-codegen/design.md}. With the per-module
     * {@code models}/{@code apis} allow-lists dropped, {@code DefaultGenerator} builds a
     * {@link CodegenModel} for every schema reachable from the document, including models that
     * exist purely as structural plumbing and were never meant to become a Java class — e.g.
     * {@code EntityModelCalendarItemDtoAllOfLinks}/{@code AllOfTemplates} (openapi-generator's own
     * naming convention for a promoted {@code Map<String, X>} value schema found while flattening an
     * envelope's {@code allOf} members) and
     * {@code CollectionModelEntityModelCalendarItemDtoEmbedded}/{@code LinksValue}/
     * {@code TemplatesValue} (the same promotion for a Shape 2 envelope, which does not use the
     * {@code AllOf} suffix at all).
     *
     * <p>These fragment models are synthesized by the generator during model construction — the raw
     * OpenAPI document (what {@link HalEnvelopeDetector#detect} inspects and what
     * {@code ModelUtils.getSchemas(openAPI)} returns) has no schema named
     * {@code EntityModelCalendarItemDtoAllOfLinks} at all; confirmed empirically against
     * {@code docs/openapi/klabis-full.json}, where {@code EntityModelCalendarItemDto._templates}
     * points at the generic, shared {@code HalFormsTemplates} schema, not at any per-model fragment
     * name. So reachability cannot be computed over the raw schema graph — it must be computed over
     * the already-built {@code objs} model map itself, using each {@link CodegenModel}'s actual
     * generated property types ({@link CodegenProperty#complexType}, including one level of
     * {@code items} for array/map-valued properties).
     *
     * <p>Every model {@link HalEnvelopeDetector#detect} flags as an envelope (by its ORIGINAL raw
     * schema — envelope names themselves are never synthesized, only their fragments are) is
     * condemned outright. From there, a fixed-point pass condemns any OTHER model whose complete
     * in-edge set (every model whose property references it) is itself already condemned — i.e. a
     * model with no path back to something that isn't already known to be plumbing. A model with NO
     * in-edges at all (nothing in the document references it as a property — the shape a real,
     * independently-named payload/request/API schema has) is never condemned this way, regardless of
     * what it in turn references. Reachability/in-edges alone — never a name fragment like
     * {@code AllOf} — decides removal.
     */
    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        Map<String, ModelsMap> result = super.postProcessAllModels(objs);
        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        Set<String> toRemove = envelopeAndFragmentNames(result, schemas);

        Set<String> survivingClassnames = new LinkedHashSet<>();
        for (Map.Entry<String, ModelsMap> entry : result.entrySet()) {
            if (toRemove.contains(entry.getKey())) {
                continue;
            }
            for (ModelMap modelMap : entry.getValue().getModels()) {
                if (modelMap.getModel() != null) {
                    survivingClassnames.add(modelMap.getModel().classname);
                }
            }
        }

        for (Iterator<Map.Entry<String, ModelsMap>> it = result.entrySet().iterator(); it.hasNext(); ) {
            if (toRemove.contains(it.next().getKey())) {
                it.remove();
            }
        }

        // openapi-generator can leave a model's `imports` list carrying a same-package classname
        // whose own model was removed above (or was never generated in the first place) — observed
        // for oneOf-branch fragments that collapse into a plain-fields record (no field of the
        // final record actually types against the import) but keep a stale entry from an earlier
        // resolution stage. Only a same-package import is a candidate: it names a class this
        // generator run itself was supposed to produce, so "not in survivingClassnames" reliably
        // means "would fail to compile", unlike a foreign-package import (jakarta.*, java.util.*,
        // Spring types, ...) which this generator run never tracks in survivingClassnames at all.
        for (ModelsMap modelsMap : result.values()) {
            List<Map<String, String>> imports = modelsMap.getImports();
            if (imports == null) {
                continue;
            }
            imports.removeIf(imp -> {
                String fqcn = imp.get("import");
                if (fqcn == null || !fqcn.startsWith(modelPackage() + ".")) {
                    return false;
                }
                String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
                return !survivingClassnames.contains(simpleName);
            });
        }

        return result;
    }

    /**
     * Every raw model-map key (e.g. {@code EntityModelCalendarItemDto_allOf__links}) that is either
     * an envelope itself or reachable only from one — see {@link #postProcessAllModels} for the full
     * rationale.
     *
     * <p>Two different name spellings have to be reconciled here: {@code objs}' own keys (and each
     * {@link CodegenModel#name}/{@code schemaName}) are the raw, unsanitized OpenAPI schema name —
     * confirmed empirically to contain underscores generated during inline-schema promotion, e.g.
     * {@code EntityModelCalendarItemDto_allOf__links} — while
     * {@link CodegenProperty#complexType} (what a referencing property actually points at) is the
     * sanitized Java class name, e.g. {@code EntityModelCalendarItemDtoAllOfLinks}. This builds the
     * reference graph keyed by {@code classname} (both a model's own identity and every property's
     * {@code complexType} are in that same sanitized form) and only translates back to the raw map
     * key at the very end, for removal from {@code objs}.
     */
    private Set<String> envelopeAndFragmentNames(Map<String, ModelsMap> objs, Map<String, Schema> schemas) {
        Map<String, String> classnameToMapKey = new LinkedHashMap<>();
        Map<String, Set<String>> referencedBy = new LinkedHashMap<>();
        for (Map.Entry<String, ModelsMap> entry : objs.entrySet()) {
            for (ModelMap modelMap : entry.getValue().getModels()) {
                CodegenModel model = modelMap.getModel();
                if (model == null) {
                    continue;
                }
                classnameToMapKey.put(model.classname, entry.getKey());
                referencedBy.put(model.classname, directlyReferencedComplexTypes(model));
            }
        }

        Set<String> envelopeClassnames = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : classnameToMapKey.entrySet()) {
            Schema<?> schema = schemas.get(entry.getValue());
            if (schema != null && HalEnvelopeDetector.detect(schema, schemas).isPresent()) {
                envelopeClassnames.add(entry.getKey());
            }
        }

        // Reverse edges: for each model, which OTHER models directly reference it.
        Map<String, Set<String>> referencedFrom = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : referencedBy.entrySet()) {
            for (String referenced : entry.getValue()) {
                if (!referenced.equals(entry.getKey())) {
                    referencedFrom.computeIfAbsent(referenced, k -> new LinkedHashSet<>()).add(entry.getKey());
                }
            }
        }

        // A model directly named by an operation's request body or response content — after the
        // SAME envelope unwrap handleMethodResponse()/fromResponse() apply — is a genuine root no
        // matter how few (or zero) OTHER models reference it as a property — e.g. CalendarItemDto
        // has exactly one in-edge (CollectionModelEntityModelCalendarItemDtoEmbedded, itself a
        // fragment) but is ALSO getCalendarItem's real, unwrapped 200 response type once
        // EntityModelCalendarItemDto is unwrapped away, so it must never be condemned. Operation
        // usage is not visible in the model-to-model CodegenProperty graph at all (an operation's
        // return type is never another model's property), so it has to be collected separately,
        // straight from the raw document.
        Set<String> operationRootClassnames = new LinkedHashSet<>(operationReferencedClassnames(classnameToMapKey, schemas));

        // A schema referenced only from an envelope's _embedded block (e.g. MemberInGroupResponse,
        // reachable only via EntityModelMembershipFeeGroupResponseWithMembers._embedded.members) is
        // ALSO a genuine root, for the same reason as above: _embedded content is assembled at
        // runtime by HalResponseContext.embed/HalResponseBodyAdvice, never as a Java field of any
        // generated type (see docs/openapi/spec/membershipfees.yaml's comment on
        // EntityModelMembershipFeeGroupResponseWithMembers) — so it is invisible both to the
        // model-property graph above AND to operationReferencedClassnames (which only looks at an
        // operation's own request/response content, never inside an envelope's _embedded shape).
        // Without this, condemning the envelope also condemns everything only _embedded reaches.
        operationRootClassnames.addAll(embeddedReferencedClassnames(envelopeClassnames, classnameToMapKey, schemas));

        // A model is a fragment if every path back to a real, independently-referenced schema
        // passes through an envelope. Computed as a fixed point: start with the envelopes
        // themselves condemned, then repeatedly condemn any OTHER model that is not an operation
        // root and whose entire in-edge set (every model that references it) is already condemned —
        // e.g. EntityModelCalendarItemDtoAllOfValue is reachable only from other already-condemned
        // fragments, so once they're condemned it is too.
        Set<String> condemned = new LinkedHashSet<>(envelopeClassnames);
        condemned.removeAll(operationRootClassnames);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String classname : classnameToMapKey.keySet()) {
                if (condemned.contains(classname) || operationRootClassnames.contains(classname)) {
                    continue;
                }
                Set<String> referrers = referencedFrom.get(classname);
                // No referrers at all (a root schema, e.g. a real payload/request/API model) is
                // never condemned — only a model whose in-edges exist AND are all condemned is.
                if (referrers != null && !referrers.isEmpty() && condemned.containsAll(referrers)) {
                    condemned.add(classname);
                    changed = true;
                }
            }
        }

        Set<String> mapKeysToRemove = new LinkedHashSet<>();
        for (String classname : condemned) {
            String mapKey = classnameToMapKey.get(classname);
            if (mapKey != null) {
                mapKeysToRemove.add(mapKey);
            }
        }
        return mapKeysToRemove;
    }

    /**
     * Classnames of every schema {@code $ref}'d from an {@code _embedded} block inside any of
     * {@code envelopeClassnames}' raw schemas — see the call site's comment for why these must be
     * treated as roots exactly like {@link #operationReferencedClassnames}. Walks one level of
     * {@code allOf} (Shape 1's payload/extension split) and one level of array items (an
     * {@code _embedded.<name>} property is always {@code array of $ref}, per
     * {@link HalEnvelopeDetector}'s Shape 1/Shape 2 rules), which is exactly the depth
     * {@code HalEnvelopeDetector} itself resolves — no deeper walk is needed because a payload
     * schema's own properties are ordinary model properties, already covered by the
     * {@code directlyReferencedComplexTypes} graph.
     */
    private Set<String> embeddedReferencedClassnames(Set<String> envelopeClassnames,
                                                       Map<String, String> classnameToMapKey,
                                                       Map<String, Schema> schemas) {
        Map<String, String> mapKeyToClassname = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : classnameToMapKey.entrySet()) {
            mapKeyToClassname.put(entry.getValue(), entry.getKey());
        }

        Set<String> refs = new LinkedHashSet<>();
        for (String envelopeClassname : envelopeClassnames) {
            String mapKey = classnameToMapKey.get(envelopeClassname);
            Schema<?> schema = mapKey == null ? null : schemas.get(mapKey);
            if (schema == null) {
                continue;
            }
            List<Schema> allOf = schema.getAllOf();
            List<Schema> candidates = allOf != null ? allOf : List.of(schema);
            for (Schema<?> candidate : candidates) {
                Map<String, Schema> properties = candidate.getProperties();
                Schema<?> embedded = properties == null ? null : properties.get("_embedded");
                if (embedded == null) {
                    continue;
                }
                if (embedded.get$ref() != null) {
                    embedded = HalEnvelopeDetector.resolveRef(embedded, schemas);
                }
                if (embedded == null || embedded.getProperties() == null) {
                    continue;
                }
                for (Schema<?> embeddedProperty : embedded.getProperties().values()) {
                    Schema<?> items = ModelUtils.isArraySchema(embeddedProperty) ? embeddedProperty.getItems() : null;
                    if (items == null || items.get$ref() == null) {
                        continue;
                    }
                    // The _embedded item ref itself is very often another envelope (e.g.
                    // PagedModelEntityModelEventSummaryDto._embedded.eventSummaryDtoList items
                    // $ref EntityModelEventSummaryDto, not the bare EventSummaryDto payload) — unwrap
                    // it the same way operationReferencedClassnames/addOperationContentRefs does, so
                    // this protects the real payload as a root instead of accidentally exempting the
                    // intermediate envelope (which would leave ITS OWN AllOf* fragments un-condemned).
                    Optional<EnvelopeUnwrap> nestedUnwrap = detectUnwrap(items, schemas);
                    Schema<?> refSchema = nestedUnwrap.map(EnvelopeUnwrap::targetSchema).orElse(items);
                    if (refSchema.get$ref() == null) {
                        continue;
                    }
                    String ref = refSchema.get$ref();
                    String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
                    String classname = mapKeyToClassname.get(schemaName);
                    if (classname != null) {
                        refs.add(classname);
                    }
                }
            }
        }
        return refs;
    }

    /**
     * Classnames of every schema directly {@code $ref}'d from an operation's request body or
     * response content anywhere in {@code openAPI.getPaths()} — the set of models
     * {@link #envelopeAndFragmentNames} must never condemn regardless of their in-edge count from
     * other models, since operation usage is invisible to the model-to-model
     * {@link CodegenProperty#complexType} graph (see the call site's comment). Deliberately does
     * NOT unwrap envelopes here (unlike {@link #handleMethodResponse}/{@link #fromResponse}):
     * whether the ref names an envelope or a bare payload, its classname is still a legitimate
     * root — an envelope ref is unwrapped via {@link HalEnvelopeDetector#detect} first, the same
     * way {@link #handleMethodResponse}/{@link #fromResponse} resolve an operation's true payload
     * type, so this set names the payload, never the envelope.
     */
    private Set<String> operationReferencedClassnames(Map<String, String> classnameToMapKey, Map<String, Schema> schemas) {
        Map<String, String> mapKeyToClassname = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : classnameToMapKey.entrySet()) {
            mapKeyToClassname.put(entry.getValue(), entry.getKey());
        }

        Set<String> refs = new LinkedHashSet<>();
        if (openAPI.getPaths() == null) {
            return refs;
        }
        for (io.swagger.v3.oas.models.PathItem pathItem : openAPI.getPaths().values()) {
            for (Operation operation : pathItem.readOperations()) {
                if (operation.getRequestBody() != null) {
                    addOperationContentRefs(operation.getRequestBody().getContent(), mapKeyToClassname, schemas, refs);
                }
                if (operation.getResponses() != null) {
                    for (ApiResponse response : operation.getResponses().values()) {
                        addOperationContentRefs(response.getContent(), mapKeyToClassname, schemas, refs);
                    }
                }
            }
        }
        return refs;
    }

    private static void addOperationContentRefs(Content content, Map<String, String> mapKeyToClassname,
                                                  Map<String, Schema> schemas, Set<String> refs) {
        if (content == null) {
            return;
        }
        for (MediaType mediaType : content.values()) {
            Schema<?> schema = mediaType == null ? null : mediaType.getSchema();
            if (schema == null) {
                continue;
            }
            Optional<EnvelopeUnwrap> unwrap = detectUnwrap(schema, schemas);
            Schema<?> effective = unwrap.map(EnvelopeUnwrap::targetSchema).orElse(schema);
            // One level of array unwrap (a bare `array of $ref`, e.g. a JSON-sibling
            // *ResponseList schema) — the same shallow unwrap depth used elsewhere in this class.
            Schema<?> refSchema = ModelUtils.isArraySchema(effective) ? effective.getItems() : effective;
            if (refSchema == null || refSchema.get$ref() == null) {
                continue;
            }
            String ref = refSchema.get$ref();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            String classname = mapKeyToClassname.get(schemaName);
            if (classname != null) {
                refs.add(classname);
            }
        }
    }

    /**
     * Sanitized class names directly referenced by {@code model} via a property's
     * {@link CodegenProperty#complexType} — including one level of {@code items} so an array-of-X
     * or {@code Map<String, X>} property still contributes X as an edge (both render as a wrapper
     * {@code CodegenProperty} whose own {@code complexType} is the container, with the
     * element/value type nested under {@code items}).
     */
    private static Set<String> directlyReferencedComplexTypes(CodegenModel model) {
        Set<String> refs = new LinkedHashSet<>();
        if (model.allVars == null) {
            return refs;
        }
        for (CodegenProperty property : model.allVars) {
            addComplexType(property, refs);
        }
        return refs;
    }

    private static void addComplexType(CodegenProperty property, Set<String> refs) {
        if (property == null) {
            return;
        }
        if (property.complexType != null) {
            refs.add(property.complexType);
        }
        addComplexType(property.items, refs);
    }

    private static boolean isPaginated(Operation operation) {
        if (operation.getExtensions() == null) {
            return false;
        }
        Object value = operation.getExtensions().get("x-spring-paginated");
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
