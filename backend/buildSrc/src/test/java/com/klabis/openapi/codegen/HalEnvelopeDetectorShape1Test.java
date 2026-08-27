package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shape 1 (single entity, {@code EntityModel<T>}) detection — see design.md Decision 3.
 */
class HalEnvelopeDetectorShape1Test {

    @Test
    void detectsEnvelopeWithLinksTemplatesAndEmbedded() {
        // EntityModelEventDtoWithRegistrations: allOf [$ref EventDto, {_links, _templates, _embedded}]
        Schema<?> eventDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> envelope = HalEnvelopeFixtures.shape1Envelope("EventDto", Map.of(
            "_embedded", new Schema<>().type("object"),
            "_links", new Schema<>().$ref("#/components/schemas/Links"),
            "_templates", new Schema<>().$ref("#/components/schemas/HalFormsTemplates")
        ));
        Map<String, Schema> schemas = Map.of("EventDto", eventDto);

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        // targetSchema must stay an unresolved $ref — handleMethodResponse() needs the schema
        // *name* (via the stock generator's own $ref resolution) to map onto the Java class.
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/EventDto");
        assertThat(result.get().isCollection()).isFalse();
    }

    @Test
    void detectsEnvelopeWithOnlyLinksAndTemplates() {
        // EntityModelPaymentRuleResponse: allOf [$ref PaymentRuleResponse, {_links, _templates}], no _embedded
        Schema<?> paymentRuleResponse = new Schema<>().type("object").addProperty("amount", new Schema<>().type("number"));
        Schema<?> envelope = HalEnvelopeFixtures.shape1Envelope("PaymentRuleResponse", Map.of(
            "_links", new Schema<>().$ref("#/components/schemas/Links"),
            "_templates", new Schema<>().$ref("#/components/schemas/HalFormsTemplates")
        ));
        Map<String, Schema> schemas = Map.of("PaymentRuleResponse", paymentRuleResponse);

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/PaymentRuleResponse");
    }

    @Test
    void detectsEnvelopeWithOnlyLinksAndTopLevelDescription() {
        // EntityModelMemberSummaryResponse: allOf [$ref MemberSummaryResponse, {_links}], plus a top-level
        // "description" sibling to allOf — detection must not be confused by extra top-level keywords.
        Schema<?> memberSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> envelope = HalEnvelopeFixtures.shape1Envelope("MemberSummaryResponse")
            .description("A member summary wrapped in a HAL entity model");
        Map<String, Schema> schemas = Map.of("MemberSummaryResponse", memberSummaryResponse);

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/MemberSummaryResponse");
    }

    // Negative-case fixtures below are deliberately hand-built, not routed through
    // HalEnvelopeFixtures: each one exists specifically to deviate from the canonical shape the
    // builder produces, so building it via the builder would hide which property makes it invalid.

    @Test
    void rejectsMarkerTypeWithOnlyLinksAndNoAllOf() {
        // EntityModelRootModel / EntityModelDashboardModel from the real spec: {type: object,
        // properties: {_links}} with NO allOf at all — these are hand-written marker types
        // (RootModel/DashboardModel) that stay explicit mappings entries, not auto-unwrap targets.
        // Neither Shape 1 (no allOf) nor Shape 2 (no _embedded-shaped property) should match.
        Schema<?> markerType = new Schema<>().type("object")
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(markerType, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsAllOfWhoseFirstMemberIsInlineNotRef() {
        // Real spec shape: EntityModelOwnerResponse / EntityModelParentResponse / EntityModelTrainerResponse
        // / EntityModelGroupMembershipResponse / EntityModelFamilyGroupMembershipResponse /
        // EntityModelFreeGroupMembershipResponse — allOf[{inline object memberId}, {_links}]. Two members,
        // second member's properties ⊆ {_links,_templates,_embedded}, but member[0] is an INLINE object,
        // not a $ref — must NOT be detected as Shape 1 (no schema name to unwrap to).
        Schema<?> envelope = new Schema<>()
            .allOf(java.util.List.of(
                new Schema<>().type("object").addProperty("memberId", new Schema<>().type("string").format("uuid")),
                new Schema<>().type("object")
                    .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
            ));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsAllOfWhoseSecondMemberHasAForeignProperty() {
        // A genuine multi-parent composition — second member has a property outside {_links, _templates,
        // _embedded} — must NOT be detected as Shape 1.
        Schema<?> base = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> envelope = new Schema<>()
            .allOf(java.util.List.of(
                new Schema<>().$ref("#/components/schemas/Base"),
                new Schema<>().type("object")
                    .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
                    .addProperty("extraBusinessField", new Schema<>().type("string"))
            ));
        Map<String, Schema> schemas = Map.of("Base", base);

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isEmpty();
    }
}
