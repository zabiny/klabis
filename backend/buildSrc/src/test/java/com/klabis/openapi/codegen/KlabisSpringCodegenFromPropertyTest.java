package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.CodegenProperty;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code fromProperty()} override — Category C (design.md Decision 3), tasks.md 6.3. Mirrors
 * {@link KlabisSpringCodegenHandleMethodResponseTest}'s rewrite-then-delegate shape, applied to a
 * model property instead of an operation's response.
 */
class KlabisSpringCodegenFromPropertyTest {

    private static KlabisSpringCodegen newCodegen(Map<String, Schema> schemas) {
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(HalEnvelopeFixtures.openApiWithSchemas(schemas));
        return codegen;
    }

    @Test
    void arrayOfShape1ItemEnvelopeResolvesToListOfPayloadNotEntityModel() {
        // FamilyGroupResponse.parents: array of $ref EntityModelParentResponse, where
        // EntityModelParentResponse = allOf[$ref ParentResponse, {_links}] (post spec-promotion shape).
        Schema<?> parentResponse = new Schema<>().type("object")
            .addProperty("memberId", new Schema<>().type("string").format("uuid"));
        Schema<?> entityModelParentResponse = HalEnvelopeFixtures.shape1Envelope("ParentResponse");

        Map<String, Schema> schemas = Map.of(
            "ParentResponse", parentResponse,
            "EntityModelParentResponse", entityModelParentResponse
        );
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Schema<?> parentsProperty = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/EntityModelParentResponse"));

        CodegenProperty property = codegen.fromProperty("parents", parentsProperty, false, false);

        assertThat(property.isArray).isTrue();
        assertThat(property.items).isNotNull();
        assertThat(property.items.baseType).isEqualTo("ParentResponse");
        assertThat(property.items.dataType).isEqualTo("ParentResponse");
        // No EntityModel wrapper anywhere in the resolved type/imports.
        assertThat(property.dataType).doesNotContain("EntityModel");
        assertThat(property.dataType).isEqualTo("List<@Valid ParentResponse>");
    }

    @Test
    void nonEnvelopePropertyIsUntouchedAndMatchesStockBehavior() {
        // A plain array-of-payload property (no envelope at all) resolves identically to stock —
        // confirms the override's fallback path delegates cleanly when no unwrap is detected.
        Schema<?> calendarItemDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Map<String, Schema> schemas = Map.of("CalendarItemDto", calendarItemDto);

        Schema<?> arrayProperty = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/CalendarItemDto"));

        KlabisSpringCodegen codegen = newCodegen(schemas);
        org.openapitools.codegen.languages.SpringCodegen stock = new org.openapitools.codegen.languages.SpringCodegen();
        stock.setOpenAPI(HalEnvelopeFixtures.openApiWithSchemas(schemas));

        CodegenProperty property = codegen.fromProperty("items", arrayProperty, false, false);
        CodegenProperty stockProperty = stock.fromProperty("items", arrayProperty, false, false);

        assertThat(property.dataType).isEqualTo(stockProperty.dataType);
        assertThat(property.items.baseType).isEqualTo(stockProperty.items.baseType);
    }

    @Test
    void nonArrayPropertyIsUntouched() {
        // A plain $ref property (e.g. AgeRangeResponse) is not an array at all — must pass through
        // unchanged, same as HalEnvelopeDetectorPropertyItemTest.rejectsNonArrayProperty().
        Schema<?> ageRangeResponse = new Schema<>().type("object").addProperty("min", new Schema<>().type("integer"));
        Map<String, Schema> schemas = Map.of("AgeRangeResponse", ageRangeResponse);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Schema<?> refProperty = new Schema<>().$ref("#/components/schemas/AgeRangeResponse");

        CodegenProperty property = codegen.fromProperty("ageRange", refProperty, false, false);

        assertThat(property.isArray).isFalse();
        assertThat(property.dataType).isEqualTo("AgeRangeResponse");
    }
}
