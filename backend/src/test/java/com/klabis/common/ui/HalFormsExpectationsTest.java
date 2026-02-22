package com.klabis.common.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

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
@Import(HalFormsExampleController.class)
class HalFormsExpectationsTest {

    @Autowired
    MockMvc mockMvc;

    @WithMockUser(username = "Tester")
    @Test
    @DisplayName("it should handle readOnly attribute for record (default as readWrite, customizable with @HalForms)")
    void shouldReturnExpectedFormsMetadata() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'id')].readOnly").value(true))
                .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'firstName')].readOnly").doesNotExist())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'lastName')].readOnly").doesNotExist())
                .andExpect(status().isOk());
    }

    @WithMockUser(username = "Tester")
    @Test
    @DisplayName("it should return expected type for DTOs")
    void shouldReturnExpectedTypeForDtoAttribute() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'address')].type").value("Address"))
                .andExpect(status().isOk());
    }

    @WithMockUser(username = "Tester")
    @Test
    @DisplayName("it should return expected type from @HalForms annotation if present")
    void shouldReturnExpectedTypeForHalFormsAnnotated() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'age')].type").value("AgeOverride"))
                .andExpect(status().isOk());
    }

}

record User(@HalForms(access = HalForms.Access.READ_ONLY) int id, String firstName, String lastName, @HalForms(formInputType = "AgeOverride") int age, Address address) {

}

record Address(String street, String city, String state, String zip) {}

@RestController
@RequestMapping("/api/testHalSupport")
class HalFormsExampleController {

    @GetMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<User>> getUser(@RequestParam int id) {
        User data = new User(2, "Petr", "Palach", 21, new Address("Ulice", "mesto", "CZ", "123456"));

        EntityModel<User> model = EntityModel.of(data)
                .add(klabisLinkTo(methodOn(HalFormsExampleController.class).getUser(id)).withSelfRel()
                        .andAffordances(klabisAfford(methodOn(HalFormsExampleController.class).editUser(id,
                                null))));

        return ResponseEntity.ok(model);
    }

    @PutMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editUser(@RequestParam int id, @RequestBody User data) {
        return ResponseEntity.noContent().build();
    }
}
