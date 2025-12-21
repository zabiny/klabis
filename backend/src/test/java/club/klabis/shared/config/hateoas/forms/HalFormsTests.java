package club.klabis.shared.config.hateoas.forms;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.adapters.api.WithMockedKlabisUser;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.HalFormsOptionItem;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import club.klabis.shared.config.security.HasMemberGrant;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.InputType;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.json.AbstractJsonContentAssert;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@DisplayName("HAL+FORMS tests")
@WithMockedKlabisUser
@ApiTestConfiguration(controllers = HalFormsTestController.class)
public class HalFormsTests {

    @Autowired
    private MockMvcTester mockMvcTester;

    private MockMvcTester.MockMvcRequestBuilder getFormsTestApi() {
        return mockMvcTester.get().uri("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE);
    }

    @Test
    @DisplayName("It should returns correctly encoded prompts for _template properties loaded from rest-default-messages.properties")
    void itShouldReturnCorrectlyEncodedFormData() throws Exception {
        assertThat(getFormsTestApi())
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.putFormData.properties[?(@.name=='name')].prompt")
                .asArray()
                .containsExactly("Jméno uživatele");
    }

    @Test
    @DisplayName("It should return correctly encoded title for template loaded from rest-default-messages.properties")
    void itShouldReturnCorrectlyEncodedTitleForTemplate() {
        assertThat(getFormsTestApi())
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.putFormData.title")
                .asString()
                .isEqualTo("Nový člen klubu");
    }


    @Test
    @DisplayName("It should honor input type defined on property using @InputType annotation")
    void itShouldHonorInputTypeAnnotation() throws Exception {
        assertThat(getFormsTestApi())
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.putFormData.properties[?(@.name=='name')].type")
                .asArray()
                .containsExactly("userName");
    }

    @Test
    @DisplayName("It should return options for enum fields")
    void itShouldReturnOptionsForDefinedFields() throws Exception {
        AbstractJsonContentAssert<?> bodyAssert = assertThat(mockMvcTester.get()
                .uri("/formsTest")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .hasStatusOk()
                .bodyJson();

        bodyAssert.extractingPath("$._templates.putFormData.properties[3].options.inline[0]")
                .convertTo(HalFormsOptionItem.class)
                .isEqualTo(new HalFormsOptionItem<>("MALE", "MALE"));
        bodyAssert.extractingPath("$._templates.putFormData.properties[3].options.inline[1]")
                .convertTo(HalFormsOptionItem.class)
                .isEqualTo(new HalFormsOptionItem<>("FEMALE", "FEMALE"));
    }

    @Disabled
    @Test
    @DisplayName("It should return options for InputOption annotated field")
    void itShouldReturnOptionsForInputOptionAnnotatedFields() throws Exception {
    }

    @Test
    @DisplayName("it should return 'boolean' for Boolean attribute")
    void itShouldReturnCorrectInputTypeForBooleanAttribute() {
        assertThat(mockMvcTester.get().uri("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.putFormData.properties[5].type")
                .isEqualTo("boolean");
    }

    @Test
    @DisplayName("it should return simple name of DTO for DTO attributes")
    void itShouldReturnSimpleDtoNameAsTypeForDtoAttributes() {
        assertThat(mockMvcTester.get().uri("/formsTest").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._templates.putFormData.properties[4].type")
                .isEqualTo("Subobject");
    }

    @DisplayName("affordBetter tests")
    @Nested
    class AffordBetterTests {

        // HAL+FORMS returns them sorted by name in default. We rather prefer them in same order as JSON value (so we can reorder it using Jackson annotations)
        @DisplayName("it should return HAL+FORM template properties in expected order")
        @Test
        void itShouldReturnTemplatePropertiesInExpectedOrder() {
            assertThat(getFormsTestApi())
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$._templates['putFormData'].properties[*].name")
                    .asArray()
                    .containsExactly("id", "name", "address", "sex", "subobject", "active");
        }


        @Test
        @DisplayName("It should NOT return record attributes as readOnly if there is not @JsonProperty(readOnly=true)")
        void itShouldHandleReadOnlyForRecords() throws Exception {
            assertThat(getFormsTestApi())
                    .hasStatusOk()
                    .bodyJson()
                    .doesNotHavePath("$._templates.putFormData.properties[0].readOnly") // id
                    .doesNotHavePath("$._templates.putFormData.properties[1].readOnly")// name
                    .hasPathSatisfying("$._templates.putFormData.properties[2].readOnly",
                            p -> p.assertThat().isEqualTo(true)); // address
        }

        @Nested
        @DisplayName("PreAuthorize affordance authorization")
        class PreAuthorizeTests {
            @Test
            @WithMockUser(roles = "ADMIN")
            @DisplayName("it should add affordance if user is authorized to use referenced controller method")
            void itShouldAddAffordanceForAuthorizedMethod() {
                assertThat(getFormsTestApi())
                        .hasStatusOk()
                        .bodyJson()
                        .extractingPath("$._templates.deleteFormDataPreAuthorize.target")
                        .isEqualTo("http://localhost/formsTest/preauthorize/1");
            }

            @Test
            @DisplayName("it should NOT add affordance if user is NOT authorized to use referenced controller method")
            void itShouldNotAddAffordanceForNotAuthorizedMethod() {
                assertThat(getFormsTestApi())
                        .hasStatusOk()
                        .bodyJson()
                        .doesNotHavePath("$._templates.deleteFormDataPreAuthorize");
            }

        }

        @Nested
        @DisplayName("HasGrant affordance authorization")
        class HasGrantTests {
            @Test
            @WithMockedKlabisUser(applicationGrants = ApplicationGrant.DEPOSIT_FINANCE)
            @DisplayName("it should add affordance if user is authorized to use referenced controller method")
            void itShouldAddAffordanceForAuthorizedMethodUsingHasGrant() {
                assertThat(getFormsTestApi())
                        .hasStatusOk()
                        .bodyJson()
                        .extractingPath("$._templates.deleteFormDataGrant.target")
                        .isEqualTo("http://localhost/formsTest/hasgrant/1");
            }

            @Test
            @DisplayName("it should NOT add affordance if user is NOT authorized to use referenced controller method")
            void itShouldNotAddAffordanceForNotAuthorizedMethod() {
                assertThat(getFormsTestApi())
                        .hasStatusOk()
                        .bodyJson()
                        .doesNotHavePath("$._templates.deleteFormDataGrant");
            }

        }

        @Nested
        @DisplayName("HasMemberGrant affordance authorization")
        class HasMemberGrantTests {
            private HasMemberGrantTests() {
            }

            @Test
            @DisplayName("it should add affordance if user is authorized to see data returned by afforded method (user is same user as affroded one)")
            @WithMockedKlabisUser(memberId = 1)
            void itShouldAddAffordanceForAuthorizedMethodUsingHasGrant() {
                assertThat(getFormsTestApi())
                        .hasStatusOk()
                        .bodyJson()
                        .extractingPath("$._templates.deleteFormDataMemberGrant.target")
                        .isEqualTo("http://localhost/formsTest/hasmembergrant/1");
            }

            @Test
            @DisplayName("it should NOT add affordance if user is NOT authorized to use referenced controller method")
            void itShouldNotAddAffordanceForNotAuthorizedMethod() {
                assertThat(getFormsTestApi())
                        .hasStatusOk()
                        .bodyJson()
                        .doesNotHavePath("$._templates.deleteFormDataMemberGrant");
            }

        }

    }

}

@ExposesResourceFor(HalFormsTestController.DataModel.class)
@ApiController(path = "/formsTest", openApiTagName = "tests")
class HalFormsTestController {

    @GetMapping
    EntityModel<DataModel> getFormData() {
        EntityModel<DataModel> result = EntityModel.of(new DataModel(1, "name", "surname", Sex.FEMALE))
                .add(WebMvcLinkBuilder.linkTo(methodOn(this.getClass()).getFormData())
                        .withSelfRel()
                        .andAffordances(affordBetter(methodOn(getClass()).putFormData(null)))
                        .andAffordances(affordBetter(methodOn(getClass()).deleteFormDataPreAuthorize(1)))
                        .andAffordances(affordBetter(methodOn(getClass()).deleteFormDataGrant(1)))
                        .andAffordances(affordBetter(methodOn(getClass()).deleteFormDataMemberGrant(new MemberId(1))))
                        .withRel("update"));

        return result;

    }

    @PutMapping
    ResponseEntity<Void> putFormData(@RequestBody DataModel data) {
        return ResponseEntity.created(URI.create("/formData/1")).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/preauthorize/{id}")
    ResponseEntity<Void> deleteFormDataPreAuthorize(@PathVariable int id) {
        return ResponseEntity.noContent().build();
    }

    @HasGrant(ApplicationGrant.DEPOSIT_FINANCE)
    @DeleteMapping("/hasgrant/{id}")
    ResponseEntity<Void> deleteFormDataGrant(@PathVariable int id) {
        return ResponseEntity.noContent().build();
    }

    @HasMemberGrant(memberId = "#memberId")
    @DeleteMapping("/hasmembergrant/{memberId}")
    ResponseEntity<Void> deleteFormDataMemberGrant(@PathVariable MemberId memberId) {
        return ResponseEntity.noContent().build();
    }

    enum Sex {MALE, FEMALE;}

    record Subobject(String name, int count) {
    }

    record DataModel(
            @JsonProperty(access = JsonProperty.Access.READ_WRITE) int id,
            @InputType("userName") String name,
            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String address,
            @InputOptions Sex sex,

            @NotNull
            Subobject subobject,

            boolean active
    ) {
        DataModel(int id, String name, String address, Sex sex) {
            this(id, name, address, sex, new Subobject("auto", 2), true);
        }
    }
}


