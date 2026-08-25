package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shape 1-item (property-level array unwrap) detection — see design.md Decision 3 (Category C) and
 * tasks.md 1.2. Mirrors the real {@code TrainingGroupResponse.trainers} shape: a model property
 * typed {@code array of $ref EntityModelTrainerResponse}, where
 * {@code EntityModelTrainerResponse = allOf[{memberId}, {_links}]} with an inline (unnamed) payload
 * half — exactly {@link HalEnvelopeDetectorShape1Test#rejectsAllOfWhoseFirstMemberIsInlineNotRef()}'s
 * negative fixture, except here the payload half has been promoted to a named schema per Decision 3
 * ("Spec changes required first: promote each inline payload half to a named schema").
 */
class HalEnvelopeDetectorPropertyItemTest {

    @Test
    void detectsArrayOfRefToShape1EnvelopeAsListOfPayload() {
        // TrainingGroupResponse.trainers: array of $ref EntityModelTrainerResponse
        Schema<?> trainerResponse = new Schema<>().type("object")
            .addProperty("memberId", new Schema<>().type("string").format("uuid"));
        Schema<?> entityModelTrainerResponse = HalEnvelopeFixtures.shape1Envelope("TrainerResponse");

        Map<String, Schema> schemas = Map.of(
            "TrainerResponse", trainerResponse,
            "EntityModelTrainerResponse", entityModelTrainerResponse
        );

        Schema<?> trainersProperty = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/EntityModelTrainerResponse"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detectPropertyItemUnwrap(trainersProperty, schemas);

        assertThat(result).isPresent();
        // targetSchema stays an unresolved $ref, same convention as detectShape1/detectShape2 —
        // fromProperty()'s own $ref resolution (via super) maps it onto the Java class name.
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/TrainerResponse");
        assertThat(result.get().isCollection()).isTrue();
    }

    @Test
    void rejectsNonArrayProperty() {
        Schema<?> plainRefProperty = new Schema<>().$ref("#/components/schemas/AgeRangeResponse");

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detectPropertyItemUnwrap(plainRefProperty, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsArrayOfPlainPayloadWithNoEnvelope() {
        // e.g. CalendarItemDtoList — array of $ref straight to the payload, no allOf/_links wrapper.
        Schema<?> calendarItemDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Map<String, Schema> schemas = Map.of("CalendarItemDto", calendarItemDto);

        Schema<?> arrayProperty = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/CalendarItemDto"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detectPropertyItemUnwrap(arrayProperty, schemas);

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsArrayOfInlineAllOfWithoutNamedPayload() {
        // Pre-promotion shape (still inline, not yet a named schema) — must NOT match, forcing the
        // spec-promotion step design.md Decision 3 requires before Category C can generate anything.
        Schema<?> inlineEnvelope = new Schema<>().allOf(List.of(
            new Schema<>().type("object").addProperty("memberId", new Schema<>().type("string").format("uuid")),
            new Schema<>().type("object").addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
        ));
        Map<String, Schema> schemas = Map.of("EntityModelTrainerResponse", inlineEnvelope);

        Schema<?> arrayProperty = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/EntityModelTrainerResponse"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detectPropertyItemUnwrap(arrayProperty, schemas);

        assertThat(result).isEmpty();
    }
}
