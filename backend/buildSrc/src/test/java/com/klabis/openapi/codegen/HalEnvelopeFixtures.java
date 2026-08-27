package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Builders for the two canonical HAL envelope shapes {@link HalEnvelopeDetector} matches — see
 * design.md Decision 3 — plus the {@code OpenAPI}/{@code Components} assembly every test needs to
 * resolve {@code $ref}s against. The detector's rules are structural, so these builders *are* the
 * specification of the rules: routing the suite's positive-case fixtures through one shared
 * implementation keeps 22 hand-built call sites from drifting as the rules change.
 *
 * <p>Only the canonical (positive-case) shapes are built here. Negative-case fixtures — schemas
 * that deliberately deviate from the canonical shape to prove the detector rejects them — stay
 * hand-built at each call site; see proposal.md's third bullet.
 */
final class HalEnvelopeFixtures {

    private HalEnvelopeFixtures() {
    }

    /**
     * Shape 1 — single entity ({@code EntityModel<T>}): {@code allOf[$ref payload, {inline object
     * of _links/_templates/_embedded}]}. {@code envelopeProperties} supplies the second allOf
     * member's properties — a subset of {@code {_links, _templates, _embedded}} — since call sites
     * vary in which of the three they include.
     */
    static Schema<?> shape1Envelope(String payloadRef, Map<String, Schema> envelopeProperties) {
        Schema<?> second = new Schema<>().type("object");
        envelopeProperties.forEach(second::addProperty);
        return new Schema<>().allOf(List.of(
            new Schema<>().$ref("#/components/schemas/" + payloadRef),
            second
        ));
    }

    /** {@link #shape1Envelope(String, Map)} with the common {@code {_links}}-only case. */
    static Schema<?> shape1Envelope(String payloadRef) {
        return shape1Envelope(payloadRef, Map.of("_links", new Schema<>().$ref("#/components/schemas/Links")));
    }

    /**
     * Shape 2 — collection ({@code PagedModel<T>}/{@code CollectionModel<T>}):
     * {@code {_embedded: {<embeddedPropertyName>: array[$ref itemRef]}, _links}}, with an optional
     * {@code page} property when {@code paged} is {@code true} — see design.md Decision 2: the
     * "page" property is deliberately ignored by the detector, so its presence must not change
     * behavior, only whether the fixture exercises the paged-sibling path.
     */
    static Schema<?> shape2Envelope(String embeddedPropertyName, String itemRef, boolean paged) {
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty(embeddedPropertyName, new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/" + itemRef)));

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));
        if (paged) {
            envelope.addProperty("page", new Schema<>().$ref("#/components/schemas/PageMetadata"));
        }
        return envelope;
    }

    /** {@link #shape2Envelope(String, String, boolean)} without a {@code page} property. */
    static Schema<?> shape2Envelope(String embeddedPropertyName, String itemRef) {
        return shape2Envelope(embeddedPropertyName, itemRef, false);
    }

    /** An {@link OpenAPI} document whose {@code components.schemas} is {@code schemas}. */
    static OpenAPI openApiWithSchemas(Map<String, Schema> schemas) {
        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();
        schemas.forEach(components::addSchemas);
        openAPI.setComponents(components);
        return openAPI;
    }
}
