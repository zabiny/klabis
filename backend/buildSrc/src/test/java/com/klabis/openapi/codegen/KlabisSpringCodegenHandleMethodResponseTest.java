package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.openapitools.codegen.CodegenModelFactory;
import org.openapitools.codegen.CodegenModelType;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.languages.SpringCodegen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code handleMethodResponse()} override — see design.md Decision 1 (envelope unwrap via
 * {@link HalEnvelopeDetector}, delegating to {@code super} for all stock type-resolution
 * machinery) and Decision 2 (pagination comes from {@code x-spring-paginated} on the operation,
 * independent of which response representation is used to resolve the payload type).
 */
class KlabisSpringCodegenHandleMethodResponseTest {

    private static OpenAPI openApiWithSchemas(Map<String, Schema> schemas) {
        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();
        schemas.forEach(components::addSchemas);
        openAPI.setComponents(components);
        return openAPI;
    }

    private static KlabisSpringCodegen newCodegen(Map<String, Schema> schemas) {
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openApiWithSchemas(schemas));
        return codegen;
    }

    private static CodegenOperation newOp() {
        return (CodegenOperation) CodegenModelFactory.newInstance(CodegenModelType.OPERATION);
    }

    private static ApiResponse jsonResponse(Schema<?> schema) {
        return new ApiResponse().content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
    }

    private static ApiResponse halResponse(Schema<?> schema) {
        return new ApiResponse().content(new Content()
            .addMediaType("application/prs.hal-json", new MediaType().schema(schema)));
    }

    private static ApiResponse multiContentResponse(Schema<?> jsonSchema, Schema<?> halSchema) {
        // insertion order matters — application/json first, matching the bundler's sort order.
        Content content = new Content();
        content.addMediaType("application/json", new MediaType().schema(jsonSchema));
        content.addMediaType("application/prs.hal-forms+json", new MediaType().schema(halSchema));
        return new ApiResponse().content(content);
    }

    private static Operation operationWithResponse(ApiResponse response, boolean paginated) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", response);
        operation.setResponses(responses);
        // Always non-null: stock SpringCodegen.fromOperation() reads operation.getExtensions()
        // without a null-check (line ~1020), so a bare `new Operation()` NPEs there even for a
        // non-paginated fixture once fromOperation() is exercised (see the 4.6 test below).
        operation.setExtensions(new java.util.LinkedHashMap<>());
        if (paginated) {
            operation.addExtension("x-spring-paginated", Boolean.TRUE);
        }
        return operation;
    }

    @Test
    void shape1EnvelopeUnwrapsToPayloadType() {
        // 4.1 — EntityModelEventDtoWithRegistrations-style envelope resolves to the unwrapped payload,
        // with no explicit schemaMappings entry.
        Schema<?> eventDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> envelope = new Schema<>().allOf(List.of(
            new Schema<>().$ref("#/components/schemas/EventDto"),
            new Schema<>().type("object").addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
        ));

        Map<String, Schema> schemas = Map.of("EventDto", eventDto, "EntityModelEventDto", envelope);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(halResponse(envelope), false);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, operation.getResponses().get("200"), Map.<String, String>of());

        assertThat(op.returnBaseType).isEqualTo("EventDto");
        assertThat(op.returnType).isEqualTo("EventDto");
        assertThat(op.returnContainer).isNull();
        assertThat(op.isArray).isFalse();
    }

    @Test
    void shape2EnvelopeWithPaginationProducesPageContainer() {
        // 4.2 — Shape 2 envelope on an x-spring-paginated: true operation -> Page<X>.
        Schema<?> memberSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("memberSummaryResponseList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/MemberSummaryResponse")));
        Schema<?> pagedEnvelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Map<String, Schema> schemas = Map.of(
            "MemberSummaryResponse", memberSummaryResponse,
            "PagedModelEntityModelMemberSummaryResponse", pagedEnvelope
        );
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(halResponse(pagedEnvelope), true);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, operation.getResponses().get("200"), Map.<String, String>of());

        assertThat(op.returnContainer).isEqualTo("Page");
        assertThat(op.returnType).isEqualTo("org.springframework.data.domain.Page<MemberSummaryResponse>");
        assertThat(op.isArray).isFalse();
    }

    @Test
    void shape2EnvelopeWithoutPaginationProducesListContainer() {
        // 4.3 — Shape 2 envelope, NOT paginated -> stock List<X> behavior, confirming the
        // no-pagination path still delegates correctly.
        Schema<?> familyGroupSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("familyGroupSummaryResponseList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/FamilyGroupSummaryResponse")));
        Schema<?> collectionEnvelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Map<String, Schema> schemas = Map.of(
            "FamilyGroupSummaryResponse", familyGroupSummaryResponse,
            "CollectionModelEntityModelFamilyGroupSummaryResponse", collectionEnvelope
        );
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(halResponse(collectionEnvelope), false);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, operation.getResponses().get("200"), Map.<String, String>of());

        // The synthesized "array" wrapper schema (see KlabisSpringCodegen.unwrappedResponseSchema())
        // resolves to containerType "array", not "list" — DefaultCodegen.handleMethodResponse()
        // treats both identically for op.isArray (line ~4613: "array".equalsIgnoreCase(...) as well
        // as "list"), which is the field that actually drives List<T> vs Page<T> selection here.
        assertThat(op.returnContainer).isEqualTo("array");
        assertThat(op.isArray).isTrue();
    }

    @Test
    void paginationAppliesEvenWhenOnlyPlainJsonIsDeclared() {
        // 4.4a — an x-spring-paginated: true operation whose response declares ONLY application/json
        // (no HAL envelope to detect) still produces Page<X> — pagination is independent of the
        // response representation (design.md Decision 2).
        Schema<?> eventSummaryDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> bareArray = new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/EventSummaryDto"));

        Map<String, Schema> schemas = Map.of("EventSummaryDto", eventSummaryDto);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(jsonResponse(bareArray), true);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, operation.getResponses().get("200"), Map.<String, String>of());

        assertThat(op.returnContainer).isEqualTo("Page");
        assertThat(op.returnType).isEqualTo("org.springframework.data.domain.Page<EventSummaryDto>");
        assertThat(op.isArray).isFalse();
    }

    @Test
    void payloadResolvesFromFirstContentEntryWhileProducesListsBoth() {
        // 4.4b — an operation declaring both application/json and HAL content resolves the payload
        // from the first content entry (application/json), while op.produces (computed separately in
        // fromOperation()/addProducesInfo(), unaffected by this override) still lists both.
        Schema<?> eventSummaryDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> bareArray = new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/EventSummaryDto"));

        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("eventSummaryDtoList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/EventSummaryDto")));
        Schema<?> halEnvelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));

        Map<String, Schema> schemas = Map.of("EventSummaryDto", eventSummaryDto);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        ApiResponse response = multiContentResponse(bareArray, halEnvelope);
        Operation operation = operationWithResponse(response, true);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, response, Map.<String, String>of());

        assertThat(op.returnContainer).isEqualTo("Page");
        assertThat(op.returnType).isEqualTo("org.springframework.data.domain.Page<EventSummaryDto>");
        assertThat(response.getContent()).containsKeys("application/json", "application/prs.hal-forms+json");
    }

    @Test
    void nonEnvelopeSchemaIsUntouchedAndMatchesStockBehavior() {
        // 4.6 — a bare, non-envelope schema (e.g. a hand-written mappings-overridden type or a plain
        // request-response record) is untouched: same op.returnType as stock SpringCodegen would
        // produce via the super delegation fallback. Exercised through the public fromOperation()
        // entry point (rather than calling handleMethodResponse() directly on a bare SpringCodegen,
        // which stays protected there) so this test also confirms the override participates
        // correctly in the real generation flow, including op.vendorExtensions population.
        Schema<?> registerMemberRequest = new Schema<>().type("object").addProperty("email", new Schema<>().type("string"));

        Map<String, Schema> schemas = Map.of("RegisterMemberRequest", registerMemberRequest);
        OpenAPI openAPI = openApiWithSchemas(schemas);

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);

        SpringCodegen stock = new SpringCodegen();
        stock.setOpenAPI(openAPI);

        ApiResponse response = jsonResponse(new Schema<>().$ref("#/components/schemas/RegisterMemberRequest"));
        Operation operation = operationWithResponse(response, false);

        CodegenOperation op = codegen.fromOperation("/api/members", "post", operation, null);
        CodegenOperation stockOp = stock.fromOperation("/api/members", "post", operation, null);

        assertThat(op.returnBaseType).isEqualTo(stockOp.returnBaseType);
        assertThat(op.returnType).isEqualTo(stockOp.returnType);
        assertThat(op.returnContainer).isEqualTo(stockOp.returnContainer);
    }
}
