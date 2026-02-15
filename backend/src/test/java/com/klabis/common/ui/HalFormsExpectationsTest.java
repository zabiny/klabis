package com.klabis.common.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static com.klabis.common.ui.HalFormsSupport.affordIfAuthorized;
import static com.klabis.common.ui.HalFormsSupport.linkToIfAuthorized;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(HalFormsExampleController.class)
class HalFormsExpectationsTest {

    @Autowired
    MockMvc mockMvc;

    @WithMockUser(username = "Tester")
    @Test
    void shouldReturnExpectedFormsMetadata() throws Exception {
        mockMvc.perform(get("/api/testHalSupport")
                        .param("id", "2")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$._templates.default.properties[2].readOnly").value(true))  // `id` is readonly
                .andExpect(jsonPath("$._templates.default.properties[0].readOnly").doesNotExist())  // or is 'false' ... `age` is NOT readonly
                .andExpect(jsonPath("$._templates.default.properties[1].readOnly").doesNotExist())  // or is 'false' ... `firstName` is NOT readonly
                .andExpect(status().isOk());
    }

}

record User(@HalForms(access = HalForms.Access.READ_ONLY) int id, String firstName, String lastName, int age) {

}

@RestController
@RequestMapping("/api/testHalSupport")
class HalFormsExampleController {

    @GetMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<User>> getUser(@RequestParam int id) {
        User data = new User(2, "Petr", "Palach", 21);

        EntityModel<User> model = EntityModel.of(data)
                .add(linkToIfAuthorized(methodOn(HalFormsExampleController.class).getUser(id)).withSelfRel()
                        .andAffordances(affordIfAuthorized(methodOn(HalFormsExampleController.class).editUser(id,
                                null))));

        return ResponseEntity.ok(model);
    }

    @PutMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editUser(@RequestParam int id, @RequestBody User data) {
        return ResponseEntity.noContent().build();
    }
}
