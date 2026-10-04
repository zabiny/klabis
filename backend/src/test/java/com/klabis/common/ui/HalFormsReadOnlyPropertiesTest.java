package com.klabis.common.ui;

import com.klabis.common.CommonInfrastructureWebMvcSetup;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetId;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HalFormsReadOnlyPropertiesTest.ReadOnlyController.class)
@DisplayName("HAL-FORMS properties readable but not changeable")
@CommonInfrastructureWebMvcSetup
class HalFormsReadOnlyPropertiesTest {

    private static final String TARGET_ID_STRING = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String OTHER_ID_STRING = "bbbbbbbb-0000-0000-0000-000000000002";

    @Autowired
    MockMvc mockMvc;

    record ReadOnlyResponse(String value) {
    }

    record ReadOnlyRequest(
            String publicField,
            @HasAuthority(Authority.MEMBERS_MANAGE) @ReadAuthority(Authority.MEMBERS_READ)
            String guardedField,
            @HasAuthority(Authority.MEMBERS_MANAGE)
            String hiddenField
    ) {
    }

    @MvcComponent
    @RestController
    static class ReadOnlyController {

        @GetMapping(value = "/api/test/read-only/{id}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<ReadOnlyResponse> get(@PathVariable UUID id) {
            EntityModel<ReadOnlyResponse> model = EntityModel.of(new ReadOnlyResponse("value"));
            klabisLinkTo(methodOn(ReadOnlyController.class).get(id)).ifPresent(link ->
                    model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(ReadOnlyController.class).update(id, null)))));
            return model;
        }

        @PatchMapping("/api/test/read-only/{id}")
        ResponseEntity<Void> update(@PathVariable @TargetId(TargetType.MEMBER) UUID id,
                                    @RequestBody ReadOnlyRequest body) {
            return ResponseEntity.noContent().build();
        }
    }

    @Test
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    @DisplayName("user who may change the field gets it editable")
    void changeableFieldIsEditable() throws Exception {
        mockMvc.perform(get("/api/test/read-only/" + OTHER_ID_STRING).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'guardedField')]").exists())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'guardedField' && @.readOnly == true)]").doesNotExist())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'hiddenField')]").exists());
    }

    @Test
    @WithKlabisMockUser(authorities = Authority.MEMBERS_READ)
    @DisplayName("user who may only read the field gets it read-only, and unreadable fields stay hidden")
    void readableFieldIsReadOnlyAndUnreadableIsHidden() throws Exception {
        mockMvc.perform(get("/api/test/read-only/" + OTHER_ID_STRING).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'publicField')]").exists())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'guardedField' && @.readOnly == true)]").exists())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'hiddenField')]").doesNotExist());
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("user with neither authority sees neither field")
    void neitherFieldWithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/test/read-only/" + OTHER_ID_STRING).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'publicField')]").exists())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'guardedField')]").doesNotExist())
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'hiddenField')]").doesNotExist());
    }

    @Test
    @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.MEMBERS_READ,
            type = TargetType.MEMBER, ids = TARGET_ID_STRING))
    @DisplayName("read authority held over the target of the invocation makes the field read-only for that target only")
    void readAuthorityOverTargetOnly() throws Exception {
        mockMvc.perform(get("/api/test/read-only/" + TARGET_ID_STRING).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'guardedField' && @.readOnly == true)]").exists());
        mockMvc.perform(get("/api/test/read-only/" + OTHER_ID_STRING).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'guardedField')]").doesNotExist());
    }
}
