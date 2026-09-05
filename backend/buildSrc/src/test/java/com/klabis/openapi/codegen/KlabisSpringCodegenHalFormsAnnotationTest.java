package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The assembled single {@code @HalForms(...)} — design Decision 1. {@code halFormsAnnotation()}
 * composes the access/formInputType attribute pairs; {@code fromModel()} runs
 * {@code postProcessModelProperty()} for real, so the end-to-end case also proves the assembled
 * string lands on the property's vendor extensions under the double {@code vars}/{@code allVars}
 * walk (two passes must not duplicate the annotation).
 */
class KlabisSpringCodegenHalFormsAnnotationTest {

    private static final String ACCESS_ONLY =
        "@com.klabis.common.ui.HalForms(access = com.klabis.common.ui.HalForms.Access.READ_ONLY)";
    private static final String INPUT_TYPE_ONLY =
        "@com.klabis.common.ui.HalForms(formInputType = \"RankingRequest\")";
    private static final String BOTH =
        "@com.klabis.common.ui.HalForms(access = com.klabis.common.ui.HalForms.Access.READ_ONLY, "
            + "formInputType = \"textarea\")";

    private static Schema<?> propertyWith(Map<String, Object> extensions) {
        Schema<?> property = new Schema<>().type("string");
        extensions.forEach(property::addExtension);
        return property;
    }

    /** Runs the real config-time pipeline so postProcessModelProperty runs through fromModel. */
    private static CodegenProperty generatedProperty(Map<String, Object> extensions) {
        Schema<?> holder = new Schema<>().type("object").addProperty("field", propertyWith(extensions));
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("test").version("1"));
        openAPI.setComponents(new Components().addSchemas("Holder", holder));

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.preprocessOpenAPI(openAPI);
        CodegenModel model = codegen.fromModel("Holder", openAPI.getComponents().getSchemas().get("Holder"));
        return model.vars.stream().filter(p -> p.name.equals("field")).findFirst().orElseThrow();
    }

    @Test
    void onlyInputTypeEmitsFormInputType() {
        assertThat(KlabisSpringCodegen.halFormsAnnotation(Map.of("x-hal-input-type", "RankingRequest")))
            .isEqualTo(INPUT_TYPE_ONLY);
    }

    @Test
    void onlyAccessEmitsAccessAndStaysByteEqualWithTheFormerMustacheRendering() {
        assertThat(KlabisSpringCodegen.halFormsAnnotation(Map.of("x-klabis-halforms-access", "READ_ONLY")))
            .isEqualTo(ACCESS_ONLY);
    }

    @Test
    void bothExtensionsComposeIntoASingleAnnotation() {
        assertThat(KlabisSpringCodegen.halFormsAnnotation(Map.of(
            "x-klabis-halforms-access", "READ_ONLY",
            "x-hal-input-type", "textarea")))
            .isEqualTo(BOTH);
    }

    @Test
    void neitherExtensionEmitsNothing() {
        assertThat(KlabisSpringCodegen.halFormsAnnotation(Map.of())).isNull();
        assertThat(KlabisSpringCodegen.halFormsAnnotation(null)).isNull();
    }

    @Test
    void generatedPropertyCarriesExactlyOneAssembledAnnotation() {
        CodegenProperty property = generatedProperty(Map.of(
            "x-klabis-halforms-access", "READ_ONLY",
            "x-hal-input-type", "textarea"));

        assertThat(property.getVendorExtensions()).containsEntry(
            "x-klabis-halforms-annotation", BOTH);
        // A second @HalForms on the same component would not compile — not @Repeatable.
        assertThat(property.getVendorExtensions().get("x-klabis-halforms-annotation"))
            .asString()
            .containsOnlyOnce("@com.klabis.common.ui.HalForms(");
    }

    @Test
    void generatedPropertyWithoutEitherExtensionGainsNoAnnotationKey() {
        CodegenProperty property = generatedProperty(Map.of());

        assertThat(property.getVendorExtensions()).doesNotContainKey("x-klabis-halforms-annotation");
    }

    @Test
    void generatedPropertyKeepsAccessOnlyRenderingOfFormerDirectTemplatePath() {
        Schema<?> holder = new Schema<>().type("object")
            .addProperty("readOnly", propertyWith(Map.of("x-klabis-halforms-access", "READ_ONLY")))
            .addProperty("typed", propertyWith(Map.of("x-hal-input-type", "RankingRequest")));

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("test").version("1"));
        openAPI.setComponents(new Components().addSchemas("Holder", holder));

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.preprocessOpenAPI(openAPI);
        CodegenModel model = codegen.fromModel("Holder", openAPI.getComponents().getSchemas().get("Holder"));

        assertThat(var(model, "readOnly").getVendorExtensions())
            .containsEntry("x-klabis-halforms-annotation", ACCESS_ONLY);
        assertThat(var(model, "typed").getVendorExtensions())
            .containsEntry("x-klabis-halforms-annotation", INPUT_TYPE_ONLY);
    }

    private static CodegenProperty var(CodegenModel model, String name) {
        return model.vars.stream().filter(p -> p.name.equals(name)).findFirst().orElseThrow();
    }
}
