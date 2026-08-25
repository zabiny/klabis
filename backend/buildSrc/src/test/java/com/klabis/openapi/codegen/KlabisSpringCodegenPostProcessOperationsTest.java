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
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for {@link KlabisSpringCodegen#postProcessOperationsWithModels}, exercised at
 * the same level {@code DefaultGenerator.processOperations()} actually calls it — aggregating
 * {@code op.imports} across ALL operations of a tag into one resolved import list before handing it
 * to postProcess. A per-operation unit test of {@code handleMethodResponse()}/{@code fromResponse()}
 * alone cannot catch this: the stray import comes from {@code DefaultCodegen.fromOperation()}'s
 * OWN content-map walk (line ~4706, {@code r.setContent(getContent(response.getContent(), ...))}),
 * which registers an import for EVERY response content-type's schema — including ones this class's
 * overrides never see, because they operate on {@code CodegenResponse}/{@code CodegenOperation}
 * fields, not on the raw {@code Content} map {@code getContent()} walks independently.
 */
class KlabisSpringCodegenPostProcessOperationsTest {

    @Test
    void collectionEnvelopeImportIsStrippedEvenWhenPayloadResolvesFromJsonSibling() {
        // Mirrors event-types' listEventTypes: application/json sibling (bare array, resolves the
        // payload/return type) declared ALONGSIDE application/prs.hal-forms+json (whose schema is
        // the Shape 2 envelope). Once --strip-hal is gone, the HAL response's real (non-blanked)
        // schema is visible to DefaultCodegen.fromOperation()'s per-content-type import walk, which
        // adds an import for the envelope even though handleMethodResponse()/fromResponse() only
        // ever unwrap the FIRST content entry (per Decision 2) and never touch this one.
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

        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();
        schemas.forEach(components::addSchemas);
        openAPI.setComponents(components);

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.setModelPackage("com.klabis.events.infrastructure.restapi");
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
        op.tags = List.of(new io.swagger.v3.oas.models.tags.Tag().name("EventTypes"));

        // Reproduce DefaultGenerator.processOperations()' own aggregation: collect op.imports across
        // all operations of the tag, resolve to FQNs, hand to postProcessOperationsWithModels — the
        // exact sequence that leaks the stray import in the real multi-operation generation run.
        OperationsMap operationsMap = new OperationsMap();
        OperationMap operationMap = new OperationMap();
        operationMap.setOperation(List.of(op));
        operationsMap.setOperation(operationMap);
        operationsMap.put("package", codegen.apiPackage());

        java.util.Set<String> allImports = new java.util.TreeSet<>();
        allImports.addAll(op.imports);
        Map<String, String> mappings = new LinkedHashMap<>();
        for (String i : allImports) {
            String mapping = codegen.importMapping().get(i);
            if (mapping != null) {
                mappings.put(mapping, i);
            } else {
                mappings.putAll(codegen.toModelImportMap(i));
            }
        }
        java.util.List<Map<String, String>> importObjects = new java.util.ArrayList<>();
        mappings.forEach((imp, cls) -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("import", imp);
            m.put("classname", cls);
            importObjects.add(m);
        });
        operationsMap.setImports(importObjects);

        OperationsMap processed = codegen.postProcessOperationsWithModels(operationsMap, List.<ModelMap>of());

        assertThat(processed.getImports())
            .extracting(m -> m.get("import"))
            .noneMatch(imp -> imp.endsWith("CollectionModelEntityModelEventTypeDto"));
    }
}
