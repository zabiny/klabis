package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shape 2 (collection, {@code PagedModel<T>}/{@code CollectionModel<T>}) detection — see
 * design.md Decision 3. Whether the eventual container is {@code Page<T>} or {@code List<T>} is
 * NOT this detector's concern (design.md Decision 2) — these tests only assert
 * {@code isCollection}, never anything about pagination.
 */
class HalEnvelopeDetectorShape2Test {

    private static Schema<?> entityModelMemberSummaryResponse() {
        // EntityModelMemberSummaryResponse: allOf [$ref MemberSummaryResponse, {_links}] — itself Shape 1.
        return new Schema<>().allOf(List.of(
            new Schema<>().$ref("#/components/schemas/MemberSummaryResponse"),
            new Schema<>().type("object").addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
        ));
    }

    @Test
    void unwrapsCollectionToInnerPayloadViaNestedShape1_withPageProperty() {
        // PagedModelEntityModelMemberSummaryResponse: _embedded.memberSummaryResponseList (array of
        // EntityModelMemberSummaryResponse, itself Shape 1) + _links + page ($ref PageMetadata).
        // The "page" property is present here on purpose but must have NO effect on the result —
        // it is deliberately ignored (see design.md Decision 2).
        Schema<?> memberSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));

        Map<String, Schema> schemas = new HashMap<>();
        schemas.put("MemberSummaryResponse", memberSummaryResponse);
        schemas.put("EntityModelMemberSummaryResponse", entityModelMemberSummaryResponse());

        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("memberSummaryResponseList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/EntityModelMemberSummaryResponse")));

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
            .addProperty("page", new Schema<>().$ref("#/components/schemas/PageMetadata"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        assertThat(result.get().isCollection()).isTrue();
        // Nested unwrap: the array item is itself a Shape 1 envelope, so the target is the inner
        // payload's $ref (MemberSummaryResponse), not the intermediate envelope's.
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/MemberSummaryResponse");
    }

    @Test
    void unwrapsCollectionToList_withoutPageProperty() {
        // CollectionModelEntityModelFamilyGroupSummaryResponse: same shape, but no "page" property —
        // must resolve identically to the "page" present case above (isCollection=true, same payload).
        Schema<?> familyGroupSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> entityModelWrapper = new Schema<>().allOf(List.of(
            new Schema<>().$ref("#/components/schemas/FamilyGroupSummaryResponse"),
            new Schema<>().type("object").addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
        ));

        Map<String, Schema> schemas = new HashMap<>();
        schemas.put("FamilyGroupSummaryResponse", familyGroupSummaryResponse);
        schemas.put("EntityModelFamilyGroupSummaryResponse", entityModelWrapper);

        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("familyGroupSummaryResponseList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/EntityModelFamilyGroupSummaryResponse")));

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        assertThat(result.get().isCollection()).isTrue();
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/FamilyGroupSummaryResponse");
    }

    @Test
    void unwrapsCollectionWhoseItemsRefPointsDirectlyAtPayload() {
        // CollectionModelEntityModelAccommodationListItemDto (real spec, the one exception among the
        // 16 Shape 2 schemas): items = $ref directly on the payload (AccommodationListItemDto), not on
        // an intermediate EntityModelX envelope. detectShape1() on the resolved item schema then finds
        // no allOf and returns empty, so the nested-unwrap fallback must keep the original item $ref.
        Schema<?> accommodationListItemDto = new Schema<>().type("object").addProperty("roomName", new Schema<>().type("string"));

        Map<String, Schema> schemas = new HashMap<>();
        schemas.put("AccommodationListItemDto", accommodationListItemDto);

        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("accommodationListItemDtoList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/AccommodationListItemDto")));

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        assertThat(result.get().isCollection()).isTrue();
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/AccommodationListItemDto");
    }

    @Test
    void unwrapsCollectionWhoseEmbeddedPropertyIsPromotedToItsOwnNamedSchema() {
        // Regression (event-types real-spec parity diff after removing --strip-hal): the openapi-
        // generator's own inline-schema resolution sometimes promotes a nested inline object into
        // its own top-level named component and replaces the property with a $ref to it — observed
        // on CollectionModelEntityModelEventTypeDto's "_embedded" property, which the real generated
        // spec renders as {$ref: "#/components/schemas/CollectionModelEntityModelEventTypeDto__embedded"}
        // instead of the inline object every other Shape 2 envelope in the spec uses. --strip-hal
        // masked this: it blanked out the whole HAL response content before the generator's
        // preprocessing ran, so this promoted-$ref shape never reached HalEnvelopeDetector before.
        // asSingleArrayOfRefProperty() must resolve a $ref'd "_embedded" property the same way it
        // reads an inline one.
        Schema<?> eventTypeDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));

        Schema<?> promotedEmbedded = new Schema<>().type("object")
            .addProperty("eventTypeDtoList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/EventTypeDto")));

        Map<String, Schema> schemas = new HashMap<>();
        schemas.put("EventTypeDto", eventTypeDto);
        schemas.put("CollectionModelEntityModelEventTypeDto__embedded", promotedEmbedded);

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", new Schema<>().$ref("#/components/schemas/CollectionModelEntityModelEventTypeDto__embedded"))
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, schemas);

        assertThat(result).isPresent();
        assertThat(result.get().isCollection()).isTrue();
        assertThat(result.get().targetSchema().get$ref()).isEqualTo("#/components/schemas/EventTypeDto");
    }

    @Test
    void rejectsMarkerTypeWithOnlyLinksAndNoEmbedded() {
        // EntityModelRootModel / EntityModelDashboardModel from the real spec: {type: object,
        // properties: {_links}} — has _links but no _embedded-shaped property at all. Must NOT be
        // detected as Shape 2 (same fixture as the Shape 1 negative test, checked here for Shape 2's
        // own matching logic).
        Schema<?> markerType = new Schema<>().type("object")
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(markerType, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsEmbeddedArrayOfInlineObjectsNotRef() {
        // Array item is an inline object, not a $ref — must NOT be detected as Shape 2.
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("items", new Schema<>().type("array").items(new Schema<>().type("object")));

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsEmbeddedPropertyNotShapedAsSingleArrayProperty() {
        // A property literally named "_embedded" exists, but it is not itself {single array-of-$ref
        // property}-shaped (it has two properties instead of one) — must NOT be detected.
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("itemsA", new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/Foo")))
            .addProperty("itemsB", new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/Bar")));

        Schema<?> envelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Optional<EnvelopeUnwrap> result = HalEnvelopeDetector.detect(envelope, Map.of());

        assertThat(result).isEmpty();
    }
}
