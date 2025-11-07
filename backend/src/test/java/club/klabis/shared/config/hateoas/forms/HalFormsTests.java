package club.klabis.shared.config.hateoas.forms;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.shared.config.restapi.ApiController;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.List;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@DisplayName("HAL+FORMS tests")
@Import(ApiTestConfiguration.class)
@WebMvcTest(controllers = TestController.class)
public class HalFormsTests {

    @Autowired
    private MockMvcTester mockMvcTester;

    private MockMvcTester.MockMvcRequestBuilder getFormsTestApi() {
        return mockMvcTester.get().uri("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE);
    }

    @Test
    @WithMockUser
    @DisplayName("It should returns correctly encoded prompts in _template data loaded from rest-default-messages.properties")
    void itShouldReturnCorrectlyEncodedFormData() throws Exception {
        assertThat(getFormsTestApi())
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.default.properties[?(@.name=='name')].prompt")
                .asArray()
                .containsExactly("Jméno uživatele");
    }


    @Test
    @WithMockUser
    @DisplayName("It should honor input type defined on property using @InputType annotation")
    void itShouldHonorInputTypeAnnotation() throws Exception {
        assertThat(getFormsTestApi())
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.default.properties[?(@.name=='name')].type")
                .asArray()
                .containsExactly("userName");
    }

    @Test
    @WithMockUser
    @DisplayName("It should return options for enum fields")
    void itShouldReturnOptionsForDefinedFields() throws Exception {
        assertThat(mockMvcTester.get().uri("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.default.properties[3].options.inline")
                .isEqualTo(List.of("MALE", "FEMALE"));
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
            assertThat(getFormsTestApi())
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$._templates['default'].properties[*].name")
                    .asArray()
                    .containsExactly("id", "name", "address", "sex");
        }


        @Test
        @WithMockUser
        @DisplayName("It should NOT return record attributes as readOnly if there is not @JsonProperty(readOnly=true)")
        void itShouldHandleReadOnlyForRecords() throws Exception {
            assertThat(getFormsTestApi())
                    .hasStatusOk()
                    .bodyJson()
                    .doesNotHavePath("$._templates.default.properties[0].readOnly") // id
                    .doesNotHavePath("$._templates.default.properties[1].readOnly")// name
                    .hasPathSatisfying("$._templates.default.properties[2].readOnly",
                            p -> p.assertThat().isEqualTo(true)); // address
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


