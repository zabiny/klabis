package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Structural detector for Klabis's two HAL envelope shapes — see
 * {@code openspec/changes/custom-openapi-codegen/design.md}, Decision 3, for the full rationale
 * and the shape-detection flowchart.
 *
 * <p>Matches by schema <em>shape</em>, never by schema name or an {@code x-klabis-*} extension:
 * this is what lets a new module's HAL envelope be recognized with zero spec changes.
 */
public final class HalEnvelopeDetector {

    private static final Set<String> ENTITY_MODEL_PROPERTIES = Set.of("_links", "_templates", "_embedded");

    private HalEnvelopeDetector() {
    }

    public static Optional<EnvelopeUnwrap> detect(Schema<?> schema, Map<String, Schema> schemas) {
        Optional<EnvelopeUnwrap> shape1 = detectShape1(schema, schemas);
        if (shape1.isPresent()) {
            return shape1;
        }
        return detectShape2(schema, schemas);
    }

    /**
     * Shape 1 — single entity ({@code EntityModel<T>}): an {@code allOf} of exactly two members
     * where member[0] is a {@code $ref} and member[1]'s properties are a subset of
     * {@code {_links, _templates, _embedded}}.
     */
    private static Optional<EnvelopeUnwrap> detectShape1(Schema<?> schema, Map<String, Schema> schemas) {
        List<Schema> allOf = schema.getAllOf();
        if (allOf == null || allOf.size() != 2) {
            return Optional.empty();
        }

        Schema<?> first = allOf.get(0);
        Schema<?> second = allOf.get(1);
        if (first.get$ref() == null) {
            return Optional.empty();
        }
        if (!isInlineObjectWithPropertiesSubsetOf(second, ENTITY_MODEL_PROPERTIES)) {
            return Optional.empty();
        }

        // Verify the $ref resolves to something real, but keep the target as the $ref schema
        // itself — handleMethodResponse() needs the unresolved $ref so the stock generator's own
        // type resolution (which reads the schema *name*, e.g. via getSimpleRef()) can map it to
        // the right Java class. An anonymous resolved schema has no name to map from.
        if (resolveRef(first, schemas) == null) {
            return Optional.empty();
        }
        return Optional.of(new EnvelopeUnwrap(first, false));
    }

    /**
     * True if {@code candidate} is an inline (non-{@code $ref}) object whose declared property
     * names are all contained in {@code allowedProperties} — used by both Shape 1 (against
     * {@code {_links, _templates, _embedded}}) and Shape 2's own sub-checks.
     */
    private static boolean isInlineObjectWithPropertiesSubsetOf(Schema<?> candidate, Set<String> allowedProperties) {
        if (candidate == null || candidate.get$ref() != null) {
            return false;
        }
        Map<String, Schema> properties = candidate.getProperties();
        if (properties == null || properties.isEmpty()) {
            return false;
        }
        return allowedProperties.containsAll(properties.keySet());
    }

    /**
     * Shape 2 — collection ({@code PagedModel<T>}/{@code CollectionModel<T>}): a plain object
     * schema with exactly one {@code _embedded}-shaped property (an inline object holding exactly
     * one array-of-{@code $ref} property — the {@code _embedded.<name>} block, key name not
     * matched) plus a property literally named {@code _links}. A {@code page} property (present on
     * {@code PagedModel*} schemas, absent on {@code CollectionModel*}) is deliberately NOT
     * inspected — see design.md Decision 2: whether the container is {@code Page<T>} or
     * {@code List<T>} is decided by the caller from the operation's {@code x-spring-paginated}
     * extension, not from this envelope's own shape.
     */
    private static Optional<EnvelopeUnwrap> detectShape2(Schema<?> schema, Map<String, Schema> schemas) {
        if (schema.getAllOf() != null) {
            return Optional.empty();
        }
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null || !properties.containsKey("_links")) {
            return Optional.empty();
        }

        // The $ref schema itself (e.g. {$ref: "#/components/schemas/EntityModelMemberSummaryResponse"}),
        // kept unresolved for the eventual target — see detectShape1()'s comment on why.
        Schema<?> itemRef = null;
        for (Schema<?> property : properties.values()) {
            Schema<?> candidate = asSingleArrayOfRefProperty(property);
            if (candidate != null) {
                itemRef = candidate;
                break;
            }
        }

        if (itemRef == null) {
            return Optional.empty();
        }

        Schema<?> resolvedItem = resolveRef(itemRef, schemas);
        if (resolvedItem == null) {
            return Optional.empty();
        }

        // The array item may itself be a Shape 1 envelope (e.g. EntityModelMemberSummaryResponse) —
        // unwrap through it so the target is the inner payload's $ref, not the intermediate envelope's.
        Optional<EnvelopeUnwrap> nested = detectShape1(resolvedItem, schemas);
        Schema<?> target = nested.map(EnvelopeUnwrap::targetSchema).orElse(itemRef);

        return Optional.of(new EnvelopeUnwrap(target, true));
    }

    /**
     * If {@code candidate} is an inline object with exactly one property that is
     * {@code type: array, items: {$ref}}, returns that item's {@code $ref} schema. Otherwise
     * {@code null}. This is the shape of the {@code _embedded.<pluralName>} block — the outer
     * property's own name is deliberately not checked by the caller.
     */
    private static Schema<?> asSingleArrayOfRefProperty(Schema<?> candidate) {
        if (candidate == null || candidate.get$ref() != null) {
            return null;
        }
        Map<String, Schema> innerProperties = candidate.getProperties();
        if (innerProperties == null || innerProperties.size() != 1) {
            return null;
        }
        Schema<?> arrayProperty = innerProperties.values().iterator().next();
        if (arrayProperty == null || !"array".equals(arrayProperty.getType())) {
            return null;
        }
        Schema<?> items = arrayProperty.getItems();
        if (items == null || items.get$ref() == null) {
            return null;
        }
        return items;
    }

    private static Schema<?> resolveRef(Schema<?> refSchema, Map<String, Schema> schemas) {
        String ref = refSchema.get$ref();
        if (ref == null) {
            return refSchema;
        }
        String name = ref.substring(ref.lastIndexOf('/') + 1);
        return schemas.get(name);
    }
}
