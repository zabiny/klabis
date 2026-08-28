package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code fromProperty()} override — the {@code x-hal-entity-items: true} marker path (design
 * Decision 3). A marked array resolves to {@code List<EntityModel<Item>>}, the exact type the
 * per-schema {@code schemaMappings} in {@code build.gradle.kts}'s {@code groups} block used to force;
 * an unmarked array is passed straight through to {@code super}.
 *
 * <p>The marker is exercised the way the real generation flow reaches it: on an array property of a
 * schema registered in {@code components.schemas}, so {@code preprocessOpenAPI} seeds the synthetic
 * {@code EntityModel<Item>} schema + mapping at config time (see that method's Javadoc on why a
 * mapping registered later, from {@code fromProperty}, comes too late to suppress the nested
 * {@code @Valid}).
 */
class KlabisSpringCodegenFromPropertyTest {

    private static OpenAPI openApiWith(Map<String, Schema> schemas) {
        OpenAPI openAPI = new OpenAPI();
        // SpringCodegen.preprocessOpenAPI() reads openAPI.getInfo().getTitle() unconditionally.
        openAPI.setInfo(new Info().title("test").version("1"));
        Components components = new Components();
        schemas.forEach(components::addSchemas);
        openAPI.setComponents(components);
        return openAPI;
    }

    private static Schema<?> markedArrayOf(String itemRef) {
        Schema<?> array = new Schema<>().type("array")
            .items(new Schema<>().$ref("#/components/schemas/" + itemRef));
        array.addExtension("x-hal-entity-items", Boolean.TRUE);
        return array;
    }

    /** Runs the config-time pipeline far enough to seed the synthetic mappings, then builds the model. */
    private static CodegenModel buildModel(OpenAPI openAPI, String modelName) {
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.preprocessOpenAPI(openAPI);
        Schema<?> schema = openAPI.getComponents().getSchemas().get(modelName);
        return codegen.fromModel(modelName, schema);
    }

    private static CodegenProperty property(CodegenModel model, String name) {
        return model.allVars.stream().filter(p -> p.name.equals(name)).findFirst().orElseThrow();
    }

    @Test
    void markedArrayResolvesToListOfEntityModelWithNoNestedValid() {
        Schema<?> ownerResponse = new Schema<>().type("object")
            .addProperty("memberId", new Schema<>().type("string").format("uuid"));
        Schema<?> groupResponse = new Schema<>().type("object")
            .addProperty("owners", markedArrayOf("OwnerResponse"));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("OwnerResponse", ownerResponse);
        schemas.put("GroupResponse", groupResponse);

        CodegenModel model = buildModel(openApiWith(schemas), "GroupResponse");
        CodegenProperty owners = property(model, "owners");

        assertThat(owners.isArray).isTrue();
        assertThat(owners.dataType).isEqualTo("List<org.springframework.hateoas.EntityModel<OwnerResponse>>");
        // The synthetic EntityModelOwnerResponse is a schemaMapping target, so the nested @Valid the
        // stock generator emits on an unmapped array item must be absent here.
        assertThat(owners.dataType).doesNotContain("@Valid");
    }

    @Test
    void markerPathSeedsSyntheticMappingBeforeModelConstruction() {
        // preprocessOpenAPI must register the synthetic EntityModel<Item> name in schemaMapping()
        // BEFORE fromModel walks the property — a mapping added later yields EntityModel<@Valid Item>.
        Schema<?> trainerResponse = new Schema<>().type("object").addProperty("memberId", new Schema<>().type("string"));
        Schema<?> trainingGroupResponse = new Schema<>().type("object")
            .addProperty("trainers", markedArrayOf("TrainerResponse"));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("TrainerResponse", trainerResponse);
        schemas.put("TrainingGroupResponse", trainingGroupResponse);

        OpenAPI openAPI = openApiWith(schemas);
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.preprocessOpenAPI(openAPI);

        assertThat(codegen.schemaMapping())
            .containsEntry("EntityModelTrainerResponse", "org.springframework.hateoas.EntityModel<TrainerResponse>");
        assertThat(openAPI.getComponents().getSchemas()).containsKey("EntityModelTrainerResponse");
    }

    @Test
    void explicitSchemaMappingOnItemRefWinsOverMarker() {
        // If the marked array's items $ref is itself an explicit schemaMapping key (a per-module
        // override, e.g. common's EntityModelRootModel -> RootModel), entityItemsArray() returns null
        // so the ORIGINAL property is delegated and the mapping is consulted rather than replaced.
        Schema<?> rootModel = new Schema<>().type("object").addProperty("x", new Schema<>().type("string"));
        Schema<?> holder = new Schema<>().type("object")
            .addProperty("roots", markedArrayOf("EntityModelRootModel"));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("RootModel", rootModel);
        schemas.put("EntityModelRootModel", new Schema<>().type("object").addProperty("x", new Schema<>().type("string")));
        schemas.put("Holder", holder);

        OpenAPI openAPI = openApiWith(schemas);
        KlabisSpringCodegen codegen = new KlabisSpringCodegen();
        codegen.setOpenAPI(openAPI);
        codegen.processOpts();
        codegen.schemaMapping().put("EntityModelRootModel", "com.klabis.common.ui.RootModel");
        codegen.importMapping().put("EntityModelRootModel", "com.klabis.common.ui.RootModel");
        codegen.preprocessOpenAPI(openAPI);

        CodegenModel model = codegen.fromModel("Holder", openAPI.getComponents().getSchemas().get("Holder"));
        CodegenProperty roots = property(model, "roots");

        // Delegated to super with the original EntityModelRootModel ref, which the explicit mapping
        // redirects onto the plain RootModel type — no synthetic EntityModel<...> wrapper.
        assertThat(roots.dataType).doesNotContain("EntityModel<");
        assertThat(roots.items.dataType).endsWith("RootModel");
        assertThat(roots.items.dataType).doesNotContain("EntityModel");
    }

    @Test
    void unmarkedArrayOfPayloadIsUntouchedAndMatchesStockBehavior() {
        Schema<?> calendarItemDto = new Schema<>().type("object").addProperty("id", new Schema<>().type("string"));
        Schema<?> holder = new Schema<>().type("object")
            .addProperty("items", new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/CalendarItemDto")));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("CalendarItemDto", calendarItemDto);
        schemas.put("Holder", holder);

        OpenAPI openAPI = openApiWith(schemas);
        CodegenModel model = buildModel(openAPI, "Holder");
        CodegenProperty items = property(model, "items");

        assertThat(items.isArray).isTrue();
        assertThat(items.dataType).doesNotContain("EntityModel");
        assertThat(items.items.baseType).isEqualTo("CalendarItemDto");
    }

    @Test
    void nonArrayPropertyIsUntouched() {
        Schema<?> ageRangeResponse = new Schema<>().type("object").addProperty("min", new Schema<>().type("integer"));
        Schema<?> holder = new Schema<>().type("object")
            .addProperty("ageRange", new Schema<>().$ref("#/components/schemas/AgeRangeResponse"));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put("AgeRangeResponse", ageRangeResponse);
        schemas.put("Holder", holder);

        CodegenModel model = buildModel(openApiWith(schemas), "Holder");
        CodegenProperty ageRange = property(model, "ageRange");

        assertThat(ageRange.isArray).isFalse();
        assertThat(ageRange.dataType).isEqualTo("AgeRangeResponse");
    }
}
