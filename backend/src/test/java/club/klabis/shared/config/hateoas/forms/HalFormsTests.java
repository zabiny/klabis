package club.klabis.shared.config.hateoas.forms;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.shared.config.restapi.ApiController;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.InputType;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HAL+FORMS tests - affordBetter method")
@Import(ApiTestConfiguration.class)
@WebMvcTest(controllers = TestController.class)
public class HalFormsTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    @DisplayName("It should returns correctly encoded prompts in _template data loaded from rest-default-messages.properties")
    void itShouldReturnCorrectlyEncodedFormData() throws Exception {
        mockMvc.perform(get("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name=='name')].prompt").value("Jméno uživatele"));
    }

    @Test
    @WithMockUser
    @DisplayName("It should NOT return record attributes as readOnly if there is not @JsonProperty(readOnly=true)")
    void itShouldHandleReadOnlyForRecords() throws Exception {
        mockMvc.perform(get("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name=='id')].readOnly").doesNotHaveJsonPath())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name=='name')].readOnly").doesNotHaveJsonPath())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name=='address')].readOnly").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("It should honor input type defined on property using @InputType annotation")
    void itShouldHonorInputTypeAnnotation() throws Exception {
        mockMvc.perform(get("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name=='name')].type").value("userName"));
    }

    static JsonPathResultMatchers templateProperty(String templateName, String propertyName, String subPath) {
        return jsonPath("$._templates.%s.properties[?(@.name=='%s')]%s".formatted(templateName, propertyName, subPath));
    }

    static JsonPathResultMatchers defaultTemplateProperty(String propertyName, String subPath) {
        return templateProperty("default", propertyName, subPath);
    }

    @Test
    @WithMockUser
    @DisplayName("It should return options for enum fields")
    void itShouldReturnOptionsForDefinedFields() throws Exception {
        mockMvc.perform(get("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(defaultTemplateProperty("sex", ".options.inline").value(Matchers.contains("MALE",
                        "FEMALE")));
    }

    @Disabled
    @Test
    @WithMockUser
    @DisplayName("It should return options for InputOption annotated field")
    void itShouldReturnOptionsForInputOptionAnnotatedFields() throws Exception {
    }


    @DisplayName("affordBetter tests")
    @Nested
    class AffordBetterTests {

        // HAL+FORMS returns them sorted by name in default. We rather prefer them in same order as JSON value (so we can reorder it using Jackson annotations)
        @DisplayName("it should return HAL+FORM template properties in expected order")
        @Test
        @WithMockUser
        void itShouldReturnTemplatePropertiesInExpectedOrder() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/formsTest").accept(MediaTypes.HAL_FORMS_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates['default'].properties[*].name",
                            Matchers.contains("id", "name", "address", "sex")));
        }
    }

}

@ExposesResourceFor(TestController.DataModel.class)
@ApiController(path = "/formsTest", openApiTagName = "tests")
class TestController {

    @GetMapping
    EntityModel<DataModel> getFormData() {
        EntityModel<DataModel> result = EntityModel.of(new DataModel(1, "name", "surname", Sex.FEMALE))
                .add(WebMvcLinkBuilder.linkTo(methodOn(this.getClass()).getFormData())
                        .withSelfRel()
                        .andAffordance(affordBetter(methodOn(getClass()).putFormData(null)))
                        .withRel("update"));

        return result;

    }

    @PutMapping
    ResponseEntity<Void> putFormData(@RequestBody DataModel data) {
        return ResponseEntity.created(URI.create("/formData/1")).build();
    }

    enum Sex {MALE, FEMALE}

    record DataModel(
            @JsonProperty(access = JsonProperty.Access.READ_WRITE) int id,
            @InputType("userName") String name,
            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String address,
            @InputOptions Sex sex
    ) {

    }
}


