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
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.languages.SpringCodegen;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code handleMethodResponse()} / {@code fromResponse()} overrides — design Decision 2: the
 * {@code Page<T>} container comes from {@code x-spring-paginated} on the operation, and the
 * springdoc {@code @Schema} doc block for such a response is suppressed because {@code Page<T>} is
 * not a legal class literal. The payload type itself is always resolved by {@code super}; these
 * overrides only wrap it / trim its doc block.
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

    private static Operation operationWithResponse(ApiResponse response, boolean paginated) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", response);
        operation.setResponses(responses);
        // Always non-null: stock SpringCodegen.fromOperation() reads operation.getExtensions()
        // without a null-check, so a bare `new Operation()` NPEs there even for a non-paginated
        // fixture once fromOperation() is exercised.
        operation.setExtensions(new java.util.LinkedHashMap<>());
        if (paginated) {
            operation.addExtension("x-spring-paginated", Boolean.TRUE);
        }
        return operation;
    }

    @Test
    void paginatedOperationProducesPageContainerFromBareArrayResponse() {
        // An x-spring-paginated: true operation whose 200 declares a bare `type: array` body
        // (listMembers / listTransactions shape) -> Page<X>.
        Schema<?> eventSummaryDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> bareArray = new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/EventSummaryDto"));

        Map<String, Schema> schemas = Map.of("EventSummaryDto", eventSummaryDto);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(jsonResponse(bareArray), true);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, operation.getResponses().get("200"), Map.<String, String>of());

        // returnContainer MUST stay unset (not "Page"): JavaSpring/returnTypes.mustache only renders
        // `{{{returnContainer}}}<{{{returnType}}}>` inside the `{{#isArray}}` branch — for a
        // non-array response it takes `{{^returnContainer}}` and renders returnType verbatim, so
        // returnType already carries the full `Page<X>` string.
        assertThat(op.returnContainer).isNull();
        assertThat(op.returnType).isEqualTo("org.springframework.data.domain.Page<EventSummaryDto>");
        assertThat(op.isArray).isFalse();
    }

    @Test
    void nonPaginatedArrayResponseKeepsStockListContainer() {
        Schema<?> familyGroupSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> bareArray = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/FamilyGroupSummaryResponse"));

        Map<String, Schema> schemas = Map.of("FamilyGroupSummaryResponse", familyGroupSummaryResponse);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(jsonResponse(bareArray), false);

        CodegenOperation op = newOp();
        codegen.handleMethodResponse(operation, schemas, op, operation.getResponses().get("200"), Map.<String, String>of());

        assertThat(op.isArray).isTrue();
        assertThat(op.returnBaseType).isEqualTo("FamilyGroupSummaryResponse");
        assertThat(op.returnType).doesNotContain("Page");
    }

    @Test
    void paginatedResponseImportsSpringDataPageNotModelPackagePage() {
        // op.imports.add("Page") in handleMethodResponse() adds only the simple name "Page".
        // DefaultGenerator resolves a simple import name via config.importMapping() first, falling
        // back to modelPackage + "." + name when absent — so without a "Page" ->
        // org.springframework.data.domain.Page entry, the generated import silently names a
        // nonexistent class in the module's OWN restapi package, which fails to compile.
        KlabisSpringCodegen codegen = newCodegen(Map.of());

        assertThat(codegen.importMapping()).containsEntry("Page", "org.springframework.data.domain.Page");
    }

    @Test
    void paginatedResponseDocBlockHasNoGenericSchemaContent() {
        // fromResponse() feeds api.mustache's per-response @ApiResponse doc block. A paginated
        // operation's true type is Page<X> — not a legal @Schema(implementation = ...) class literal
        // — so CodegenResponse.baseType (which gates {{#baseType}} in api.mustache) must stay unset.
        Schema<?> memberSummaryResponse = new Schema<>().type("object").addProperty("name", new Schema<>().type("string"));
        Schema<?> bareArray = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/MemberSummaryResponse"));

        Map<String, Schema> schemas = Map.of("MemberSummaryResponse", memberSummaryResponse);
        KlabisSpringCodegen codegen = newCodegen(schemas);

        Operation operation = operationWithResponse(jsonResponse(bareArray), true);

        // fromResponse(code, response) has no access to the enclosing Operation — exercised through
        // fromOperation() (like the stock flow) rather than called directly, so the paginated-ness
        // recorded from the surrounding operation is actually in effect.
        CodegenOperation op = codegen.fromOperation("/api/members", "get", operation, null);
        CodegenResponse r = op.responses.stream()
            .filter(candidate -> "200".equals(candidate.code))
            .findFirst()
            .orElseThrow();

        assertThat(r.baseType).isNull();
    }

    @Test
    void schemaMappedOntoJavaLangObjectHasNoDocBlockContent() {
        // suspendMember's 409 response is schemaMappings'd onto java.lang.Object (SuspensionBlockedWarning
        // has no Java type standing for its oneOf union). springdoc renders Object.class as
        // {"type": "string"} — worse than no content block — so KlabisSpringCodegen suppresses it.
        Schema<?> suspensionBlockedWarning = new Schema<>().type("object").addProperty("reason", new Schema<>().type("string"));
        Map<String, Schema> schemas = Map.of("SuspensionBlockedWarning", suspensionBlockedWarning);
        KlabisSpringCodegen codegen = newCodegen(schemas);
        codegen.schemaMapping().put("SuspensionBlockedWarning", "java.lang.Object");

        ApiResponse response = jsonResponse(new Schema<>().$ref("#/components/schemas/SuspensionBlockedWarning"));
        Operation operation = operationWithResponse(response, false);

        CodegenOperation op = codegen.fromOperation("/api/members/{id}/suspend", "post", operation, null);
        CodegenResponse r = op.responses.stream()
            .filter(candidate -> "200".equals(candidate.code))
            .findFirst()
            .orElseThrow();

        assertThat(r.baseType).isNull();
    }

    @Test
    void nonPaginatedNonArraySchemaIsUntouchedAndMatchesStockBehavior() {
        // A bare, non-paginated schema is untouched: same op.returnType as stock SpringCodegen would
        // produce via the super delegation. Exercised through the public fromOperation() entry point
        // so this also confirms the override participates correctly in the real generation flow.
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
