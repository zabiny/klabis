package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code x-klabis-authority} accepts a single name or a list, but the templates can only emit the
 * comma-separated {@code @HasAuthority({...})} array from a list — mustache cannot tell the last
 * element of a bare string. The codegen therefore hands the templates a list in both cases.
 */
class KlabisSpringCodegenAuthorityListTest {

    private static KlabisSpringCodegen newCodegen(OpenAPI openAPI) {
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        return codegen;
    }

    private static OpenAPI emptyOpenApi() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("test").version("1"));
        openAPI.setComponents(new Components());
        return openAPI;
    }

    private static CodegenOperation operationWith(Map<String, Object> extensions) {
        Operation operation = new Operation();
        operation.setResponses(new ApiResponses());
        operation.setExtensions(new LinkedHashMap<>(extensions));
        return newCodegen(emptyOpenApi()).fromOperation("/api/members", "post", operation, null);
    }

    private static Map<String, Object> propertyExtensions(Map<String, Object> extensions) {
        OpenAPI openAPI = emptyOpenApi();
        KlabisSpringCodegen codegen = newCodegen(openAPI);
        Schema<?> property = new Schema<>().type("string");
        extensions.forEach(property::addExtension);
        Schema<?> holder = new Schema<>().type("object").addProperty("field", property);
        openAPI.getComponents().addSchemas("Holder", holder);
        codegen.preprocessOpenAPI(openAPI);
        CodegenModel model = codegen.fromModel("Holder", holder);
        return model.vars.get(0).getVendorExtensions();
    }

    @Test
    void operationAuthorityGivenAsStringBecomesAList() {
        assertThat(operationWith(Map.of("x-klabis-authority", "MEMBERS_MANAGE")).vendorExtensions)
            .containsEntry("x-klabis-authority", List.of("MEMBERS_MANAGE"));
    }

    @Test
    void operationAuthorityGivenAsListStaysAList() {
        assertThat(operationWith(Map.of("x-klabis-authority", List.of("MEMBERS_MANAGE", "EVENTS_MANAGE"))).vendorExtensions)
            .containsEntry("x-klabis-authority", List.of("MEMBERS_MANAGE", "EVENTS_MANAGE"));
    }

    @Test
    void operationWithoutAuthorityGainsNoKey() {
        assertThat(operationWith(Map.of()).vendorExtensions).doesNotContainKey("x-klabis-authority");
    }

    @Test
    void propertyAuthorityAndReadAuthorityGivenAsStringBecomeLists() {
        assertThat(propertyExtensions(Map.of(
            "x-klabis-authority", "MEMBERS_MANAGE",
            "x-klabis-read-authority", "MEMBERS_READ")))
            .containsEntry("x-klabis-authority", List.of("MEMBERS_MANAGE"))
            .containsEntry("x-klabis-read-authority", List.of("MEMBERS_READ"));
    }

    @Test
    void propertyAuthorityGivenAsListStaysAList() {
        assertThat(propertyExtensions(Map.of("x-klabis-authority", List.of("MEMBERS_MANAGE", "MEMBERS_READ"))))
            .containsEntry("x-klabis-authority", List.of("MEMBERS_MANAGE", "MEMBERS_READ"));
    }
}
