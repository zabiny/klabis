package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.openapitools.codegen.CodegenOperation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code fromOperation()} override — {@code addDerivedHalFormsContentType()}. The bundler derives
 * the {@code application/prs.hal-forms+json} content entry into {@code klabis-full.json} from the
 * {@code application/json} payload (see {@code tools/openapi-bundle/lib/derive.mjs}), but
 * {@code produces} on the generated interface is built by {@code DefaultCodegen}'s {@code private}
 * {@code addProducesInfo()} straight from the response content-map keys. This override re-adds the
 * media type for the same set of responses the deriver walks, so a spec that no longer spells the
 * media type out still produces an interface that answers the {@code Accept} header the frontend
 * sends.
 */
class KlabisSpringCodegenProducesTest {

    private static final String HAL_FORMS = "application/prs.hal-forms+json";

    private static KlabisSpringCodegen newCodegen() {
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(HalEnvelopeFixtures.openApiWithSchemas(Map.of(
            "MemberAccountResource", new Schema<>().type("object").addProperty("balance", new Schema<>().type("number"))
        )));
        return codegen;
    }

    private static Operation operation(ApiResponses responses, Map<String, Object> extensions) {
        Operation operation = new Operation();
        operation.setResponses(responses);
        operation.setExtensions(new java.util.LinkedHashMap<>(extensions));
        return operation;
    }

    private static ApiResponse jsonResponse() {
        return new ApiResponse().content(new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/MemberAccountResource"))));
    }

    private static List<String> producedMediaTypes(CodegenOperation op) {
        return op.produces == null ? List.of() : op.produces.stream().map(m -> m.get("mediaType")).toList();
    }

    @Test
    void addsHalFormsToProducesForA2xxJsonResponse() {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", jsonResponse());
        KlabisSpringCodegen codegen = newCodegen();

        CodegenOperation op = codegen.fromOperation("/api/members/{id}/account", "get",
            operation(responses, Map.of()), null);

        assertThat(producedMediaTypes(op)).contains(HAL_FORMS);
    }

    @Test
    void doesNotAddHalFormsWhenOperationOptsOutWithXKlabisHalFalse() {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", jsonResponse());
        KlabisSpringCodegen codegen = newCodegen();

        CodegenOperation op = codegen.fromOperation("/api/oris/events", "get",
            operation(responses, Map.of("x-klabis-hal", Boolean.FALSE)), null);

        assertThat(producedMediaTypes(op)).doesNotContain(HAL_FORMS);
    }

    @Test
    void doesNotAddHalFormsToANonSuccessResponse() {
        ApiResponses responses = new ApiResponses();
        // A plain application/json 409 (e.g. suspendMember's SuspensionBlockedWarning) is not a
        // hypermedia resource — the deriver skips it, and so does this override.
        responses.addApiResponse("409", jsonResponse());
        KlabisSpringCodegen codegen = newCodegen();

        CodegenOperation op = codegen.fromOperation("/api/members/{id}/suspend", "post",
            operation(responses, Map.of()), null);

        assertThat(producedMediaTypes(op)).doesNotContain(HAL_FORMS);
    }

    @Test
    void doesNotDuplicateAnAlreadyDeclaredHalFormsEntry() {
        Content content = new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/MemberAccountResource")))
            .addMediaType(HAL_FORMS, new MediaType());
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse().content(content));
        KlabisSpringCodegen codegen = newCodegen();

        CodegenOperation op = codegen.fromOperation("/api/members/{id}/account", "get",
            operation(responses, Map.of()), null);

        assertThat(producedMediaTypes(op)).filteredOn(HAL_FORMS::equals).hasSize(1);
    }

    @Test
    void doesNotAddHalFormsToABodylessSuccessResponse() {
        // A 201 with no application/json sibling has nothing to wrap. Asserted on a response that
        // does not already declare the media type, so a synthesized entry would be visible: with a
        // bodyless 201's own hal-forms entry present, an override that wrongly fired would be
        // indistinguishable from one that correctly did nothing.
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("201", new ApiResponse().content(new Content()));
        KlabisSpringCodegen codegen = newCodegen();

        CodegenOperation op = codegen.fromOperation("/api/members/{id}/account/deposits", "post",
            operation(responses, Map.of()), null);

        assertThat(producedMediaTypes(op)).doesNotContain(HAL_FORMS);
    }

    @Test
    void keepsABodylessResponsesOwnHalFormsEntryExactlyOnce() {
        // The 61 bodyless 201/204 entries the migration leaves in the specs: the media type is
        // advertised because the spec declares it, and the override must not double it up.
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("201", new ApiResponse()
            .content(new Content().addMediaType(HAL_FORMS, new MediaType())));
        KlabisSpringCodegen codegen = newCodegen();

        CodegenOperation op = codegen.fromOperation("/api/members/{id}/account/deposits", "post",
            operation(responses, Map.of()), null);

        assertThat(producedMediaTypes(op)).filteredOn(HAL_FORMS::equals).hasSize(1);
    }
}
