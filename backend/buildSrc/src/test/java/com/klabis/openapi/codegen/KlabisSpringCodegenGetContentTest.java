package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getContent()} override — see proposal.md's "getContent() override replaces the
 * name-based import cleanup". {@code DefaultCodegen.fromOperation()} calls {@code getContent()}
 * directly on the response's raw content map (line ~4707, BEFORE either {@link
 * KlabisSpringCodegen#fromResponse} or {@link KlabisSpringCodegen#handleMethodResponse} ever
 * unwraps anything), adding an import for every media type's schema — including the HAL envelope,
 * which is never generated as a Java class. This test exercises {@code getContent()} directly,
 * the same level {@code fromOperation()} calls it at, and asserts on the {@code imports} set it
 * populates as a side effect.
 */
class KlabisSpringCodegenGetContentTest {

    private static KlabisSpringCodegen newCodegen(Map<String, Schema> schemas) {
        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();
        schemas.forEach(components::addSchemas);
        openAPI.setComponents(components);

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.setModelPackage("com.klabis.events.infrastructure.restapi");
        return codegen;
    }

    @Test
    void collectionEnvelopeContributesNoImportWhenJsonSiblingIsPresent() {
        // Mirrors event-types' listEventTypes: application/json sibling (bare array, resolves the
        // payload/return type) declared ALONGSIDE application/prs.hal-forms+json (whose schema is
        // the Shape 2 envelope). getContent() walks every content entry, so without the override
        // it adds an import for the envelope too, even though handleMethodResponse()/fromResponse()
        // only ever unwrap the FIRST content entry.
        Schema<?> eventTypeDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("eventTypeDtoList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/EventTypeDto")));
        Schema<?> collectionEnvelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));
        Schema<?> bareArray = new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/EventTypeDto"));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("EventTypeDto", eventTypeDto);
        schemas.put("CollectionModelEntityModelEventTypeDto", collectionEnvelope);
        schemas.put("EventTypeDtoList", bareArray);

        KlabisSpringCodegen codegen = newCodegen(schemas);

        Content content = new Content();
        content.addMediaType("application/json", new MediaType().schema(new Schema<>().$ref("#/components/schemas/EventTypeDtoList")));
        content.addMediaType("application/prs.hal-forms+json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/CollectionModelEntityModelEventTypeDto")));

        Set<String> imports = new TreeSet<>();
        codegen.getContent(content, imports, "200ResponseBody");

        assertThat(imports).noneMatch(imp -> imp.contains("CollectionModelEntityModelEventTypeDto"));
    }

    @Test
    void singleItemEnvelopeWithNoJsonSiblingContributesNoImport() {
        // Mirrors groupsFamily/groupsFree/groupsTraining: a SOLE application/prs.hal-forms+json
        // entry, no application/json sibling — Shape 1 envelope.
        Schema<?> memberSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> envelope = new Schema<>().allOf(List.of(
            new Schema<>().$ref("#/components/schemas/MemberSummaryResponse"),
            new Schema<>().type("object").addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
        ));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("MemberSummaryResponse", memberSummaryResponse);
        schemas.put("EntityModelMemberSummaryResponse", envelope);

        KlabisSpringCodegen codegen = newCodegen(schemas);

        Content content = new Content();
        content.addMediaType("application/prs.hal-forms+json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/EntityModelMemberSummaryResponse")));

        Set<String> imports = new TreeSet<>();
        codegen.getContent(content, imports, "200ResponseBody");

        assertThat(imports).noneMatch(imp -> imp.contains("EntityModelMemberSummaryResponse"));
        assertThat(imports).anyMatch(imp -> imp.contains("MemberSummaryResponse"));
    }

    @Test
    void fromOperationNeverAddsEnvelopeImportForPromotedEmbeddedRef() {
        // Regression coverage carried over from the deleted KlabisSpringCodegenPostProcessOperationsTest:
        // event-types' listEventTypes exercised through the real fromOperation() entry point, where
        // the generator's own inline-schema resolution had promoted the _embedded block into its own
        // top-level component ($ref, not inline) — the case that originally caught this bug. Now that
        // getContent() unwraps before super sees it, no leftover import should reach op.imports either.
        Schema<?> eventTypeDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> entityModelEventTypeDto = new Schema<>().allOf(List.of(
            new Schema<>().$ref("#/components/schemas/EventTypeDto"),
            new Schema<>().type("object").addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"))
        ));
        Schema<?> embedded = new Schema<>().type("object")
            .addProperty("eventTypeDtoList", new Schema<>().type("array")
                .items(new Schema<>().$ref("#/components/schemas/EntityModelEventTypeDto")));
        Schema<?> collectionEnvelope = new Schema<>().type("object")
            .addProperty("_embedded", embedded)
            .addProperty("_links", new Schema<>().$ref("#/components/schemas/Links"));
        Schema<?> bareArray = new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/EventTypeDto"));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("EventTypeDto", eventTypeDto);
        schemas.put("EntityModelEventTypeDto", entityModelEventTypeDto);
        schemas.put("CollectionModelEntityModelEventTypeDto", collectionEnvelope);
        schemas.put("EventTypeDtoList", bareArray);

        KlabisSpringCodegen codegen = newCodegen(schemas);
        codegen.setApiPackage("com.klabis.events.infrastructure.restapi");

        Content content = new Content();
        content.addMediaType("application/json", new MediaType().schema(new Schema<>().$ref("#/components/schemas/EventTypeDtoList")));
        content.addMediaType("application/prs.hal-forms+json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/CollectionModelEntityModelEventTypeDto")));
        ApiResponse response = new ApiResponse().content(content);

        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", response);
        operation.setResponses(responses);
        operation.setExtensions(new LinkedHashMap<>());
        operation.setOperationId("listEventTypes");
        operation.setTags(List.of("EventTypes"));

        CodegenOperation op = codegen.fromOperation("/api/event-types", "get", operation, null);

        assertThat(op.imports).noneMatch(imp -> imp.contains("CollectionModelEntityModelEventTypeDto"));
    }

    @Test
    void nonEnvelopeContentIsUntouched() {
        Schema<?> registerMemberRequest = new Schema<>().type("object").addProperty("email", new Schema<>().type("string"));
        Map<String, Schema> schemas = Map.of("RegisterMemberRequest", registerMemberRequest);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Content content = new Content();
        content.addMediaType("application/json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/RegisterMemberRequest")));

        Set<String> imports = new TreeSet<>();
        codegen.getContent(content, imports, "200ResponseBody");

        assertThat(imports).anyMatch(imp -> imp.contains("RegisterMemberRequest"));
    }
}
