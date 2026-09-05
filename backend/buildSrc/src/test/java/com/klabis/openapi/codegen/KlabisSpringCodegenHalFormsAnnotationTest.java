package com.klabis.openapi.codegen;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The assembled single {@code @HalForms(...)} — design Decision 1. {@code halFormsAnnotation()}
 * composes the access/formInputType attribute pairs; {@code fromModel()} runs
 * {@code postProcessModelProperty()} for real, so the end-to-end case also proves the assembled
 * string lands on the property's vendor extensions under the double {@code vars}/{@code allVars}
 * walk (two passes must not duplicate the annotation). The rendering tests compile the live
 * {@code pojo.mustache} so a regression in the template branch (19 live access-only properties have
 * no other guard) fails here instead of silently dropping annotations from generated code.
 */
class KlabisSpringCodegenHalFormsAnnotationTest {

    private static final String ACCESS_ONLY =
        "@com.klabis.common.ui.HalForms(access = com.klabis.common.ui.HalForms.Access.READ_ONLY)";
    private static final String INPUT_TYPE_ONLY =
        "@com.klabis.common.ui.HalForms(formInputType = \"RankingRequest\")";
    private static final String BOTH =
        "@com.klabis.common.ui.HalForms(access = com.klabis.common.ui.HalForms.Access.READ_ONLY, "
            + "formInputType = \"textarea\")";
    private static final Pattern TAG = Pattern.compile("\\{\\{([#^][A-Za-z_][A-Za-z0-9_.-]*|[A-Za-z_][A-Za-z0-9_.-]*)\\}\\}");
    private static final Pattern PARTIAL = Pattern.compile("\\{\\{>\\s*([A-Za-z_][A-Za-z0-9_.-]*)\\s*\\}\\}");

    private static Schema<?> propertyWith(Map<String, Object> extensions) {
        Schema<?> property = new Schema<>().type("string");
        extensions.forEach(property::addExtension);
        return property;
    }

    /** Runs the real config-time pipeline so postProcessModelProperty runs through fromModel. */
    private static CodegenModel generatedModel(Map<String, Map<String, Object>> properties) {
        Schema<?> holder = new Schema<>().type("object");
        properties.forEach((name, extensions) -> holder.addProperty(name, propertyWith(extensions)));
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("test").version("1"));
        openAPI.setComponents(new Components().addSchemas("Holder", holder));

        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.preprocessOpenAPI(openAPI);
        return codegen.fromModel("Holder", openAPI.getComponents().getSchemas().get("Holder"));
    }

    private static CodegenProperty generatedProperty(Map<String, Object> extensions) {
        return var(generatedModel(Map.of("field", extensions)), "field");
    }

    private static CodegenProperty var(CodegenModel model, String name) {
        return model.vars.stream().filter(p -> p.name.equals(name)).findFirst().orElseThrow();
    }

    /** Renders the live pojo.mustache component line for a single property with these extensions. */
    private static String renderedComponent(Map<String, Object> extensions) {
        Map<String, Object> property = Map.of(
            "vendorExtensions", extensions.isEmpty() ? Map.of() : Map.of("x-klabis-halforms-annotation",
                KlabisSpringCodegen.halFormsAnnotation(extensions)),
            "datatypeWithEnum", "UpdateEventRankingRequest",
            "name", "ranking",
            "isNullable", false);
        try {
            // jmustache throws on any name absent from the context, even with nullValue(""); every
            // name the template (and its compile-time-inlined partials) references is seeded null,
            // which nullValue("") renders empty and keeps unguarded sections falsy.
            Map<String, Object> context = new HashMap<>();
            harvestNames(readTemplate("pojo"), context, new HashSet<>());
            context.put("vars", List.of(property));
            context.put("useBeanValidation", false);
            context.put("openApiNullable", true);
            Template template = Mustache.compiler().escapeHTML(false).nullValue("")
                .withLoader(name -> new StringReader(readTemplate(name)))
                .compile(readTemplate("pojo"));
            return template.execute(context);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Partials the Klabis template dir does not override are read from the generator's built-in
     * library at codegen time; none of them touches the annotation chain, so an empty stub stands
     * in for them here.
     */
    private static String readTemplate(String name) throws IOException {
        Path local = templateDir().resolve(name + ".mustache");
        return Files.exists(local) ? Files.readString(local) : "";
    }

    private static void harvestNames(String template, Map<String, Object> context, Set<String> visited)
        throws IOException {
        if (!visited.add(template)) {
            return;
        }
        Matcher tag = TAG.matcher(template);
        while (tag.find()) {
            if (tag.group(1) != null) {
                context.putIfAbsent(tag.group(1), null);
            }
        }
        Matcher partial = PARTIAL.matcher(template);
        while (partial.find()) {
            harvestNames(readTemplate(partial.group(1)), context, visited);
        }
    }

    private static Path templateDir() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("src/main/openapi-templates");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("src/main/openapi-templates not found above " + Path.of("").toAbsolutePath());
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
        CodegenModel model = generatedModel(Map.of(
            "readOnly", Map.of("x-klabis-halforms-access", "READ_ONLY"),
            "typed", Map.of("x-hal-input-type", "RankingRequest")));

        assertThat(var(model, "readOnly").getVendorExtensions())
            .containsEntry("x-klabis-halforms-annotation", ACCESS_ONLY);
        assertThat(var(model, "typed").getVendorExtensions())
            .containsEntry("x-klabis-halforms-annotation", INPUT_TYPE_ONLY);
    }

    @Test
    void templateRendersTheAccessOnlyAnnotation() {
        assertThat(renderedComponent(Map.of("x-klabis-halforms-access", "READ_ONLY")))
            .contains(ACCESS_ONLY)
            .containsOnlyOnce("@com.klabis.common.ui.HalForms(");
    }

    @Test
    void templateRendersTheInputTypeOnlyAnnotation() {
        assertThat(renderedComponent(Map.of("x-hal-input-type", "RankingRequest")))
            .contains(INPUT_TYPE_ONLY)
            .containsOnlyOnce("@com.klabis.common.ui.HalForms(");
    }

    @Test
    void templateRendersBothExtensionsAsASingleAnnotation() {
        assertThat(renderedComponent(Map.of(
            "x-klabis-halforms-access", "READ_ONLY",
            "x-hal-input-type", "textarea")))
            .contains(BOTH)
            .containsOnlyOnce("@com.klabis.common.ui.HalForms(");
    }

    @Test
    void templateRendersNoHalFormsAnnotationWithoutExtensions() {
        assertThat(renderedComponent(Map.of()))
            .doesNotContain("@com.klabis.common.ui.HalForms(");
    }
}
