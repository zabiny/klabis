package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.patch.PatchField;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({HalFormsExampleController.class, ExamplePostprocessor.class})
class HalFormsExpectationsTest {

    @Autowired
    MockMvc mockMvc;

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("it should handle readOnly attribute for record (default as readWrite, customizable with @HalForms)")
    void shouldReturnExpectedFormsMetadata() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'id')].readOnly").value(true))
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'firstName')].readOnly").doesNotExist())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'lastName')].readOnly").doesNotExist())
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("it should return expected type for DTOs")
    void shouldReturnExpectedTypeForDtoAttribute() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'address')].type").value("Address"))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("it should return expected type for Optional attributes")
    void shouldReturnExpectedTypeForOptionalAttribute() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'dietary')].type").value("text"))
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'postalAddress')].type").value("Address"))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("it should return expected type for PatchField attributes")
    void shouldReturnExpectedTypeForPatchFieldAttribute() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'diet')].type").value("text"))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("it should return expected type from @HalForms annotation if present")
    void shouldReturnExpectedTypeForHalFormsAnnotated() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'age')].type").value("AgeOverride"))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Set<String> field should produce type 'text' with multi: true")
    void shouldUnwrapSetOfStringToTextWithMultiple() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'tags')].type").value("text"))
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'tags')].multi").value(true))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Set<Authority> field should produce type 'Authority' with multi: true")
    void shouldUnwrapSetOfEnumToEnumTypeWithMultiple() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'roles')].type").value("Authority"))
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'roles')].multi").value(true))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("List<Integer> field should produce type 'number' with multi: true")
    void shouldUnwrapListOfIntegerToNumberWithMultiple() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'scores')].type").value("number"))
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'scores')].multi").value(true))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Set<Address> field should produce type 'Address' with multi: true")
    void shouldUnwrapSetOfCompositeTypeToTypeNameWithMultiple() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'addresses')].type").value("Address"))
                .andExpect(jsonPath("$._templates.editUser.properties[?(@.name == 'addresses')].multi").value(true))
                .andExpect(status().isOk());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("ModelWithDomainPostprocessor is invoked for EntityModelWithDomain and adds links")
    void shouldInvokePostprocessorForEntityModelWithDomain() throws Exception {
        mockMvc.perform(get("/api/testHalSupport/withDomain")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.editUser").exists());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("domainItem is not serialized in response body")
    void shouldNotSerializeDomainItem() throws Exception {
        mockMvc.perform(get("/api/testHalSupport/withDomain")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainItem").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("postprocessor receives domain item with controller-supplied data")
    void shouldPassDomainItemToPostprocessor() throws Exception {
        mockMvc.perform(get("/api/testHalSupport/withDomain")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links['domain-marker'].href").value("/domain/admin"));
    }

}

record User(@HalForms(access = HalForms.Access.READ_ONLY) int id, String firstName, String lastName, @HalForms(formInputType = "AgeOverride") int age, Address address, Optional<String> dietary, Optional<Address> postalAddress, PatchField<String> diet, Set<String> tags, Set<Authority> roles, List<Integer> scores, Set<Address> addresses) {

}

record Address(String street, String city, String state, String zip) {}

record UserEntity(User userData, List<String> authorities) {}

@RestController
@RequestMapping("/api/testHalSupport")
class HalFormsExampleController {

    private static final Logger LOG = LoggerFactory.getLogger(HalFormsExampleController.class);

    @GetMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<User>> getUser(@RequestParam int id) {
        User data = new User(2, "Petr", "Palach", 21, new Address("Ulice", "mesto", "CZ", "123456"), Optional.of("Vegetarian"), Optional.empty(), PatchField.of("Jen maso"), Set.of("tag1"), Set.of(Authority.MEMBERS_READ), List.of(42), Set.of());

        EntityModel<User> model = EntityModel.of(data);
        klabisLinkTo(methodOn(HalFormsExampleController.class).getUser(id)).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(HalFormsExampleController.class).editUser(id, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/withDomain", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<User>> getUserWithDomain(@RequestParam int id) {
        User data = new User(id, "Petr", "Palach", 21, new Address("Ulice", "mesto", "CZ", "123456"),
                Optional.of("Vegetarian"), Optional.empty(), PatchField.of("Jen maso"), Set.of("tag1"),
                Set.of(Authority.MEMBERS_READ), List.of(42), Set.of());
        UserEntity domain = new UserEntity(data, List.of("admin"));

        return ResponseEntity.ok(entityModelWithDomain(data, domain));
    }

    @PutMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editUser(@RequestParam int id, @RequestBody User data) {
        LOG.info("Test edit of user with id {} and data {}", id, data);
        return ResponseEntity.noContent().build();
    }
}

@Component
class ExamplePostprocessor extends ModelWithDomainPostprocessor<User, UserEntity> {

    @Override
    public void process(EntityModel<User> dtoModel, UserEntity domain) {
        int userId = domain.userData().id();

        klabisLinkTo(methodOn(HalFormsExampleController.class).getUserWithDomain(userId)).ifPresent(link ->
                dtoModel.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(HalFormsExampleController.class).editUser(userId, null)))));

        String marker = domain.authorities().contains("admin") ? "/domain/admin" : "/domain/user";
        dtoModel.add(org.springframework.hateoas.Link.of(marker, "domain-marker"));
    }
}
