package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import java.util.List;
import java.util.Map;

/**
 * Klabis-specific fork of the stock {@code spring} OpenAPI generator.
 *
 * <p>Vendor fork, diff on upgrade: this class overrides specific protected/public extension points
 * of {@link SpringCodegen} to teach the generator two Klabis conventions the stock generator has no
 * concept of. Both are read from explicit declarations in the hand-written module spec — the
 * generator never sees a HAL envelope schema at all, because {@code tools/openapi-bundle} reconstructs
 * those into {@code klabis-full.json} for the frontend and Swagger UI only. See
 * {@code openspec/changes/derive-hal-envelopes-in-bundler/design.md}.
 *
 * <ul>
 *     <li><b>{@code x-hal-entity-items: true}</b> on an array property — its items are independently
 *     addressable resources, so the property must resolve to {@code List<EntityModel<Item>>} rather
 *     than {@code List<Item>}. {@link #preprocessOpenAPI} seeds a synthetic {@code EntityModel<Item>}
 *     schema mapping; {@link #fromProperty} rewrites the array's items {@code $ref} onto it. Design
 *     Decision 3. Counterpart of {@code derive.mjs}'s {@code deriveEntityItems}.</li>
 *     <li><b>{@code x-spring-paginated: true}</b> on an operation — the return type is
 *     {@code Page<T>}. {@link #handleMethodResponse} applies the container after {@code super} has
 *     resolved the payload type; {@link #fromResponse} suppresses the springdoc {@code @Schema}
 *     doc block for the response, since a generic {@code Page<T>} is not a legal class literal.
 *     Design Decision 2.</li>
 * </ul>
 *
 * <p>{@link #fromOperation} additionally re-adds {@value #HAL_FORMS_MEDIA_TYPE} to a HAL response's
 * {@code produces} clause — the bundler derives that content entry from the {@code application/json}
 * payload, but {@code produces} is built from the response content-map keys before any override here
 * runs. See {@link #addDerivedHalFormsContentType}.
 *
 * <p>Every override here exists to keep one property true: the method signature and its Javadoc/doc
 * comment always agree about the return type.
 */
public class KlabisSpringCodegen extends SpringCodegen {

    private static final String HAL_FORMS_MEDIA_TYPE = "application/prs.hal-forms+json";

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

    private static final String ENTITY_MODEL_FQN = "org.springframework.hateoas.EntityModel";

    public KlabisSpringCodegen() {
        // "Page" is the simple name handleMethodResponse() adds to op.imports when an operation is
        // paginated (see the override below). DefaultGenerator resolves a simple import name via
        // importMapping() first, falling back to modelPackage + "." + name when absent — without
        // this entry the generated import silently named a nonexistent class in the module's own
        // restapi package instead of Spring Data's Page, which failed to compile. Same registration
        // point SpringCodegen itself uses for "Pageable" (see SpringCodegen.processOpts()).
        importMapping.put("Page", "org.springframework.data.domain.Page");
        // Same registration for EntityModel — fromProperty() resolves an x-hal-entity-items array to
        // List<EntityModel<Item>> by pointing the array's items at a synthetic mapped schema name
        // whose schemaMapping()/importMapping() value carries the EntityModel<Item> generic. The
        // bare "EntityModel" import name must resolve to the Spring HATEOAS class the same way.
        importMapping.put("EntityModel", ENTITY_MODEL_FQN);
    }

    @Override
    public String getName() {
        return "klabis-spring";
    }

    /**
     * Registers the synthetic {@code EntityModel<Item>} schema mappings for every
     * {@code x-hal-entity-items: true} array declared as a DIRECT property of a top-level
     * {@code components.schemas} entry, BEFORE model construction begins.
     *
     * <p>That one-level scan covers every marker in the spec today (all of them sit on the
     * {@code FamilyGroupResponse}/{@code GroupResponse}/{@code TrainingGroupResponse} payloads). A
     * marker buried in a nested object, an {@code allOf} member or an array's items would be missed
     * here and silently fall through to {@link #fromProperty}'s no-op path, producing
     * {@code List<Item>} instead of {@code List<EntityModel<Item>>} — recurse here before putting one
     * there.
     *
     * <p>{@link #fromProperty} could add these on the fly, but the stock generator decides whether
     * to emit a nested {@code @Valid} on an array's item type from state it builds while walking the
     * schemas — a mapping registered only once {@code fromProperty} runs comes too late for that
     * decision, and the item comes out as {@code EntityModel<@Valid Item>} instead of the
     * {@code EntityModel<Item>} the hand-written {@code schemaMappings} produced. Seeding here
     * reproduces that config-time timing exactly. Counterpart of {@code derive.mjs}'s
     * {@code deriveEntityItems}.
     */
    @Override
    public void preprocessOpenAPI(io.swagger.v3.oas.models.OpenAPI openAPI) {
        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        if (schemas != null) {
            for (Schema<?> schema : Map.copyOf(schemas).values()) {
                registerEntityItemsMappings(schema, schemas);
            }
        }
        super.preprocessOpenAPI(openAPI);
    }

    /**
     * For each {@code x-hal-entity-items: true} array property on {@code schema}, reinstates the
     * {@code EntityModel<Item>} setup the removed hand-written {@code schemaMappings} provided:
     * <ul>
     *     <li>a {@code schemaMapping()}/{@code importMapping()} entry redirecting the synthetic
     *     {@code EntityModelItem} name onto {@code org.springframework.hateoas.EntityModel<Item>};</li>
     *     <li>a minimal {@code EntityModelItem} schema ({@code allOf: [Item]}) in the document. Only
     *     its existence matters, not its shape: the nested {@code @Valid} on an array item is
     *     suppressed only when the item {@code $ref} both resolves AND is mapped — a mapped-but-absent
     *     name still yields {@code EntityModel<@Valid Item>}. Confirmed empirically against the
     *     baseline output in task 7.3a; being mapped, this schema is never emitted as a Java file, so
     *     it needs none of the {@code _links}/{@code _templates} the wire envelope carries.</li>
     * </ul>
     */
    private void registerEntityItemsMappings(Schema<?> schema, Map<String, Schema> schemas) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        for (Object propertyObj : schema.getProperties().values()) {
            Schema<?> property = (Schema<?>) propertyObj;
            String itemName = entityItemsRefName(property);
            // Same guard as entityItemsArray: an explicit schemaMappings entry on the item $ref wins
            // over the marker, so seeding a synthetic mapping for it would register a name nothing
            // ever resolves. Both halves of the marker path must agree on when it applies.
            if (itemName == null || schemaMapping().containsKey(itemName)) {
                continue;
            }
            String syntheticName = syntheticEntityModelName(itemName);
            schemaMapping().putIfAbsent(syntheticName, ENTITY_MODEL_FQN + "<" + itemName + ">");
            importMapping().putIfAbsent(syntheticName, ENTITY_MODEL_FQN);
            schemas.putIfAbsent(syntheticName, new Schema<>().addAllOfItem(
                new Schema<>().$ref("#/components/schemas/" + itemName)));
        }
    }

    /**
     * The bare payload schema name an {@code x-hal-entity-items: true} array's items {@code $ref}
     * points at, or {@code null} when {@code p} is not such an array. {@code validate.mjs} rejects a
     * malformed marker, so a well-formed spec always yields either {@code null} or a name.
     */
    private static String entityItemsRefName(Schema<?> p) {
        if (p == null || p.getExtensions() == null
            || !Boolean.TRUE.equals(p.getExtensions().get("x-hal-entity-items"))) {
            return null;
        }
        if (!ModelUtils.isArraySchema(p) || p.getItems() == null || p.getItems().get$ref() == null) {
            return null;
        }
        String ref = p.getItems().get$ref();
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        currentOperation = operation;
        try {
            addDerivedHalFormsContentType(operation);
            return super.fromOperation(path, httpMethod, operation, servers);
        } finally {
            currentOperation = null;
        }
    }

    /**
     * Ensures every HAL response advertises {@value #HAL_FORMS_MEDIA_TYPE} in the generated
     * interface's {@code produces} clause, even when the source spec no longer spells the media type
     * out.
     *
     * <p>The bundler ({@code tools/openapi-bundle/lib/derive.mjs}) derives the HAL envelope schema
     * and its {@code application/prs.hal-forms+json} content entry into {@code klabis-full.json}
     * from the {@code application/json} payload alone. But
     * {@code produces} is not built from the schema — {@code DefaultCodegen.fromOperation()} calls
     * the {@code private} {@code addProducesInfo()} for each response BEFORE any override here runs,
     * and it reads the response's content-map key set verbatim. A response whose only content entry
     * is {@code application/json} would therefore drop {@code application/prs.hal-forms+json} from
     * {@code produces}, and — {@code MemberAccountController}'s class-level
     * {@code @RequestMapping(produces = HAL_FORMS_JSON_VALUE)} being overridden by the method-level
     * clause — answer 406 to the {@code Accept} header the frontend sends.
     *
     * <p>So the same derivation happens here: a bare content key with no schema is added to each
     * qualifying response, which is exactly what {@code addProducesInfo()} consumes.
     *
     * <p><b>The "is this a HAL response" test in {@link #isHalResponse} must stay in agreement with
     * {@code derive.mjs}'s {@code forEachHalResponse}</b> — the two are the same rule on either side
     * of the language boundary. Change one, change the other.
     */
    private void addDerivedHalFormsContentType(Operation operation) {
        if (isHalOptedOut(operation) || operation.getResponses() == null) {
            return;
        }
        for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
            if (!isHalResponse(entry.getKey(), entry.getValue())) {
                continue;
            }
            Content content = entry.getValue().getContent();
            if (!content.containsKey(HAL_FORMS_MEDIA_TYPE)) {
                content.addMediaType(HAL_FORMS_MEDIA_TYPE, new MediaType());
            }
        }
    }

    /**
     * A response the bundler would derive a HAL envelope for, and whose generated interface must
     * therefore advertise {@value #HAL_FORMS_MEDIA_TYPE}. Counterpart of {@code derive.mjs}'s
     * {@code forEachHalResponse} — see {@link #addDerivedHalFormsContentType}; the two must agree.
     *
     * <p>The rule: a {@code 2xx} response carrying an {@code application/json} entry with a schema.
     * Error bodies ({@code problem+json}, or {@code suspendMember}'s plain-JSON 409 warning) are not
     * hypermedia resources; a response with no {@code application/json} schema (a bodyless 201/204)
     * has nothing to wrap. The operation-level opt-out is checked by the caller.
     */
    private static boolean isHalResponse(String statusCode, ApiResponse response) {
        if (!statusCode.matches("2\\d\\d") || response == null || response.getContent() == null) {
            return false;
        }
        MediaType json = response.getContent().get("application/json");
        return json != null && json.getSchema() != null;
    }

    /**
     * True when the operation carries {@code x-klabis-hal: false} — the sole opt-out from HAL being
     * added by default. Counterpart of the same check in {@code derive.mjs}'s {@code forEachHalResponse}.
     *
     * <p>Matches boolean {@code false} only, exactly as the deriver's {@code === false} does.
     * {@code validate.mjs} rejects the extension with any other value, so the bundle this reads can
     * only ever carry the boolean — accepting a {@code "false"} string here would be tolerance the
     * deriver does not share, and the two must not drift.
     */
    private static boolean isHalOptedOut(Operation operation) {
        if (operation.getExtensions() == null) {
            return false;
        }
        return Boolean.FALSE.equals(operation.getExtensions().get("x-klabis-hal"));
    }

    /**
     * Applies the {@code Page<T>} container when the operation declares {@code x-spring-paginated:
     * true} — see design Decision 2. The payload type itself is resolved entirely by {@code super};
     * this override only wraps it afterwards, so an operation serving a bare {@code application/json}
     * array still gets its pagination metadata.
     *
     * <p>Widened to {@code public} (legal on override) so it is directly unit-testable against
     * in-memory {@link Operation}/{@link ApiResponse} fixtures.
     */
    @Override
    public void handleMethodResponse(Operation operation,
                                      Map<String, Schema> schemas,
                                      CodegenOperation op,
                                      ApiResponse methodResponse,
                                      Map<String, String> schemaMappings) {
        super.handleMethodResponse(operation, schemas, op, methodResponse, schemaMappings);

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
     * Rewrites an {@code x-hal-entity-items: true} array property so its resolved Java type is
     * {@code List<org.springframework.hateoas.EntityModel<Item>>} — the exact type the per-schema
     * {@code schemaMappings} in {@code build.gradle.kts}'s {@code groups} block used to force. The
     * synthetic {@code EntityModelItem} schema and its mapping are set up by {@link #preprocessOpenAPI};
     * here the array's {@code items.$ref} is simply pointed at that name, so the stock generator
     * substitutes it exactly as it did the hand-written entries. HAL {@code _links} per item stay a
     * runtime concern of the existing {@code RepresentationModelProcessor}/{@code EntityModel.of(...)}
     * call sites, untouched by this rewrite.
     *
     * <p><b>An explicit {@code schemaMappings} entry on the item {@code $ref} wins.</b> When the
     * array item's {@code $ref} names a schema that is itself a key in {@code schemaMapping()} (e.g.
     * {@code common}'s {@code EntityModelRootModel} -> {@code RootModel}), this override delegates the
     * ORIGINAL property to {@code super} so the mapping is consulted rather than overridden.
     *
     * <p>Otherwise the property passes straight through to {@code super} unchanged — the generator has
     * no HAL envelope concept, and none reaches it (the bundler consumes every envelope schema before
     * {@code klabis-full.json}, and the codegen reads the module spec, not the bundle).
     */
    @Override
    public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean schemaIsFromAdditionalProperties) {
        Schema<?> entityItems = entityItemsArray(p);
        return super.fromProperty(name, entityItems != null ? entityItems : p,
            required, schemaIsFromAdditionalProperties);
    }

    /**
     * When {@code p} is an {@code x-hal-entity-items: true} array whose {@code items} is a
     * {@code $ref} to schema {@code Item}, returns a copy of the array whose items {@code $ref}
     * points at the synthetic {@code EntityModel<Item>} schema name — registered in
     * {@code schemaMapping()}/{@code importMapping()} by {@link #preprocessOpenAPI} — so
     * {@code super.fromProperty} resolves the property to
     * {@code List<org.springframework.hateoas.EntityModel<Item>>}. Returns {@code null} when the
     * marker is absent or malformed (validated in {@code validate.mjs}), or when the item {@code $ref}
     * is itself an explicit {@code schemaMapping()} key (an explicit per-module override wins).
     *
     * <p>Reads {@code x-hal-entity-items} straight off the source YAML (the codegen never sees the
     * bundle, where the deriver has already consumed and stripped it). Counterpart of
     * {@code derive.mjs}'s {@code deriveEntityItems}.
     */
    private Schema<?> entityItemsArray(Schema<?> p) {
        String itemName = entityItemsRefName(p);
        if (itemName == null || schemaMapping().containsKey(itemName)) {
            return null;
        }
        // Same key shape as the static "EntityModelParentResponse" -> "EntityModel<ParentResponse>"
        // entries this replaces: a plain schema name, no angle brackets, so ModelUtils' $ref parsing
        // and schemaMapping() lookup behave exactly as they did for the hand-written entries.
        return new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/" + syntheticEntityModelName(itemName)));
    }

    /**
     * The name of the synthetic envelope schema standing in for {@code EntityModel<Item>}. Must
     * match {@code derive.mjs}'s {@code EntityModel${itemName}} so the bundle and the generated Java
     * agree on which payload an enveloped item refers to.
     */
    private static String syntheticEntityModelName(String itemName) {
        return "EntityModel" + itemName;
    }

    /**
     * Suppresses the springdoc {@code @Schema(implementation = ...)} doc block for a response whose
     * true Java type cannot be written as a class literal — a paginated collection ({@code Page<T>})
     * or a {@code schemaMappings} target of {@code java.lang.Object}. {@code fromOperation()} builds
     * every {@code CodegenResponse} BEFORE {@code handleMethodResponse()} applies the {@code Page<T>}
     * container, so without this the method signature would say {@code Page<X>} while its
     * {@code @Schema} still named the bare payload — or springdoc would render {@code Object.class}
     * as {@code "type": "string"}. Clearing {@code baseType} makes {@code api.mustache}'s
     * {@code {{#baseType}}} section never open, so no illegal literal is emitted.
     */
    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        CodegenResponse r = super.fromResponse(responseCode, response);

        Schema<?> responseSchema = resolveResponseSchema(response);
        boolean paginatedCollection = currentOperation != null
            && isPaginated(currentOperation)
            && responseSchema != null
            && ModelUtils.isArraySchema(responseSchema);

        if (paginatedCollection || "Object".equals(r.baseType) || "java.lang.Object".equals(r.baseType)) {
            r.baseType = null;
        }
        return r;
    }

    /**
     * The schema of the response's first content entry (the bundler sorts {@code application/json}
     * first wherever it exists) — used only to tell {@link #fromResponse} whether a paginated
     * operation's body is a collection.
     */
    private Schema<?> resolveResponseSchema(ApiResponse response) {
        Content content = response.getContent();
        if (content == null || content.isEmpty()) {
            return null;
        }
        MediaType first = content.values().iterator().next();
        if (first == null || first.getSchema() == null) {
            return null;
        }
        Schema<?> schema = first.getSchema();
        if (schema.get$ref() == null) {
            return schema;
        }
        String ref = schema.get$ref();
        Map<String, Schema> schemas = ModelUtils.getSchemas(openAPI);
        return schemas == null ? null : schemas.get(ref.substring(ref.lastIndexOf('/') + 1));
    }

    private static boolean isPaginated(Operation operation) {
        if (operation.getExtensions() == null) {
            return false;
        }
        Object value = operation.getExtensions().get("x-spring-paginated");
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
