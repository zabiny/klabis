package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code x-klabis-nullable} flip — design Decision 2. Exercised through the real
 * {@code fromModel()} pipeline because the ordering IS the behavior: the flip must land before
 * {@code super.postProcessModelProperty()} collects the {@code JsonNullable} import, so a
 * {@code $ref} + {@code x-klabis-nullable: true} property ends up indistinguishable from the
 * {@code type: [x, "null"]} spelling it replaces (Decision 6 zero-diff).
 */
class KlabisSpringCodegenExplicitNullableTest {

    private static final String JSON_NULLABLE_HELPER_KEY = "x-jackson-optional-nullable-helpers";

    /** {@code setNullable(true)} is the parsed shape of the OpenAPI 3.1 {@code type: [x, "null"]}. */
    private static Schema<?> stockNullableProperty() {
        Schema<?> property = new Schema<>().type("string");
        property.setNullable(Boolean.TRUE);
        return property;
    }

    private static Schema<?> markedRefProperty(String ref, boolean nullable) {
        Schema<?> property = new Schema<>().$ref("#/components/schemas/" + ref);
        property.addExtension("x-klabis-nullable", nullable);
        return property;
    }

    private static Schema<?> refProperty(String ref) {
        return new Schema<>().$ref("#/components/schemas/" + ref);
    }

    private static CodegenModel buildModel(Map<String, Schema> schemas, String modelName) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("test").version("1"));
        Components components = new Components();
        schemas.forEach(components::addSchemas);
        openAPI.setComponents(components);

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.preprocessOpenAPI(openAPI);
        return codegen.fromModel(modelName, openAPI.getComponents().getSchemas().get(modelName));
    }

    private static CodegenProperty var(CodegenModel model, String name) {
        return model.vars.stream().filter(p -> p.name.equals(name)).findFirst().orElseThrow();
    }

    @Test
    void trueOnRefPropertyReproducesTheStockTypeArrayFlags() {
        Schema<?> stockRankingRef = refProperty("Ranking");
        stockRankingRef.setNullable(Boolean.TRUE);
        Schema<?> ranking = new Schema<>().type("object")
            .addProperty("points", new Schema<>().type("integer"));

        CodegenModel marked = buildModel(new LinkedHashMap<>(Map.of(
            "Ranking", ranking,
            "UpdateEventRequest", new Schema<>().type("object")
                .addProperty("ranking", markedRefProperty("Ranking", true)))), "UpdateEventRequest");
        CodegenModel stock = buildModel(new LinkedHashMap<>(Map.of(
            "Ranking", ranking,
            "UpdateEventRequest", new Schema<>().type("object")
                .addProperty("ranking", stockRankingRef))), "UpdateEventRequest");

        CodegenProperty markedRanking = var(marked, "ranking");
        CodegenProperty stockRanking = var(stock, "ranking");

        assertThat(markedRanking.isNullable).isTrue();
        // Same resolved type either way — pojo.mustache adds the single JsonNullable<...> wrapper.
        assertThat(markedRanking.datatypeWithEnum)
            .isEqualTo(stockRanking.datatypeWithEnum)
            .isEqualTo("Ranking");
        // The import super collects from the flipped flag is what model.mustache's imports loop
        // renders — the wrapper type would otherwise reference an unimported class. Identical flag
        // sets mean the refactored spelling generates byte-identical files (Decision 6).
        assertThat(marked.imports).contains("JsonNullable");
        assertThat(marked.imports).isEqualTo(stock.imports);
        assertThat(marked.getVendorExtensions()).containsEntry(JSON_NULLABLE_HELPER_KEY, Boolean.TRUE);
    }

    @Test
    void trueOnRefEnumPropertyLeavesTheSingleWrapToTheTemplate() {
        Schema<?> statusEnum = new StringSchema().addEnumItem("OPEN").addEnumItem("CLOSED");

        CodegenModel model = buildModel(new LinkedHashMap<>(Map.of(
            "StatusEnum", statusEnum,
            "Holder", new Schema<>().type("object")
                .addProperty("status", markedRefProperty("StatusEnum", true)))), "Holder");
        CodegenProperty status = var(model, "status");

        assertThat(status.isNullable).isTrue();
        // A $ref to an enum schema is a MODEL reference (isEnum stays false — inline enums are the
        // test below); the property resolves to the bare enum model name and pojo.mustache wraps it
        // exactly once via its openApiNullable section. A second wrap would not compile.
        assertThat(status.datatypeWithEnum)
            .isEqualTo("StatusEnum")
            .doesNotContain("JsonNullable");
        assertThat(model.imports).contains("JsonNullable");
    }

    @Test
    void trueOnInlineEnumProperty() {
        // stock-consistency only; validate forbids authoring this (inline enum must stay inside
        // its holder schema, the directive belongs on the $ref property).
        Schema<?> inlineEnum = new StringSchema().addEnumItem("A").addEnumItem("B");
        inlineEnum.addExtension("x-klabis-nullable", Boolean.TRUE);

        CodegenModel model = buildModel(Map.of("Holder", new Schema<>().type("object")
            .addProperty("kind", inlineEnum)), "Holder");
        CodegenProperty kind = var(model, "kind");

        assertThat(kind.isNullable).isTrue();
        assertThat(kind.isEnum).isTrue();
        assertThat(kind.datatypeWithEnum).doesNotContain("JsonNullable");
    }

    @Test
    void falseSuppressesTheWrapperOnANullableWireType() {
        Schema<?> maybe = stockNullableProperty();
        maybe.addExtension("x-klabis-nullable", Boolean.FALSE);

        CodegenModel model = buildModel(Map.of("Holder", new Schema<>().type("object")
            .addProperty("maybe", maybe)), "Holder");

        assertThat(var(model, "maybe").isNullable).isFalse();
        // Flipped BEFORE super ran, so no JsonNullable leftovers from the stock-nullable flag.
        assertThat(model.imports).doesNotContain("JsonNullable");
        assertThat(model.getVendorExtensions()).doesNotContainKey(JSON_NULLABLE_HELPER_KEY);
    }

    @Test
    void absentExtensionKeepsStockNullabilityUntouched() {
        CodegenModel model = buildModel(Map.of("Holder", new Schema<>().type("object")
            .addProperty("nullableField", stockNullableProperty())
            .addProperty("plainField", new Schema<>().type("string"))), "Holder");

        assertThat(var(model, "nullableField").isNullable).isTrue();
        assertThat(var(model, "plainField").isNullable).isFalse();
        assertThat(model.imports).contains("JsonNullable");
    }

    @Test
    void refTargetExtensionFallsThroughLikeEveryPropertyExtension() {
        // stock-consistency only; validate forbids authoring this (the directive belongs on the
        // property node, never inside the shared target). fromProperty copies the $ref TARGET's
        // extensions when the property node carries none — x-klabis-nullable inherits that stock
        // rule instead of special-casing it. The ranking case (iteration 5) always carries the
        // extension on the property node itself.
        Schema<?> shared = new Schema<>().type("object")
            .addProperty("points", new Schema<>().type("integer"));
        shared.addExtension("x-klabis-nullable", Boolean.TRUE);

        CodegenModel model = buildModel(new LinkedHashMap<>(Map.of(
            "Shared", shared,
            "Holder", new Schema<>().type("object")
                .addProperty("ranking", refProperty("Shared")))), "Holder");

        assertThat(var(model, "ranking").isNullable).isTrue();
    }

    @Test
    void propertyExtensionWinsOverRefTarget() {
        // stock-consistency only; validate forbids authoring this (false on a $ref property is a
        // request-body shape validate rejects, and a target carrying the directive alongside is
        // doubly so).
        Schema<?> shared = new Schema<>().type("object")
            .addProperty("points", new Schema<>().type("integer"));
        shared.addExtension("x-klabis-nullable", Boolean.TRUE);
        Schema<?> property = refProperty("Shared");
        property.addExtension("x-klabis-nullable", Boolean.FALSE);

        CodegenModel model = buildModel(new LinkedHashMap<>(Map.of(
            "Shared", shared,
            "Holder", new Schema<>().type("object")
                .addProperty("ranking", property))), "Holder");

        assertThat(var(model, "ranking").isNullable).isFalse();
    }

    @Test
    void trueOnRequiredPropertyAnchorsTheStockMissingImportHole() {
        // Anchor of the current state, not an endorsement: super collects the JsonNullable import
        // only for a non-required property, while the template wraps on isNullable alone — so a
        // required nullable property generates a JsonNullable wrapper without the import (a
        // compile error). validate.mjs forbids authoring this; the test pins the codegen as
        // stock-consistent while that rule is the only guard.
        Schema<?> holder = new Schema<>().type("object")
            .addProperty("ranking", markedRefProperty("Ranking", true));
        holder.addRequiredItem("ranking");

        CodegenModel model = buildModel(new LinkedHashMap<>(Map.of(
            "Ranking", new Schema<>().type("object")
                .addProperty("points", new Schema<>().type("integer")),
            "Holder", holder)), "Holder");

        CodegenProperty ranking = var(model, "ranking");
        assertThat(ranking.isNullable).isTrue();
        assertThat(ranking.datatypeWithEnum).isEqualTo("Ranking");
        assertThat(model.imports).doesNotContain("JsonNullable");
    }

    @Test
    void helperReadsStrictBooleansOnly() {
        assertThat(KlabisSpringCodegen.explicitNullable(Map.of("x-klabis-nullable", Boolean.TRUE)))
            .isEqualTo(Boolean.TRUE);
        assertThat(KlabisSpringCodegen.explicitNullable(Map.of("x-klabis-nullable", Boolean.FALSE)))
            .isEqualTo(Boolean.FALSE);
        // Non-boolean values are left for validate.mjs to reject, not silently coerced here.
        assertThat(KlabisSpringCodegen.explicitNullable(Map.of("x-klabis-nullable", "true"))).isNull();
        assertThat(KlabisSpringCodegen.explicitNullable(Map.of())).isNull();
        assertThat(KlabisSpringCodegen.explicitNullable(null)).isNull();
    }
}
