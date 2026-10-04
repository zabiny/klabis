package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.CommonInfrastructureWebMvcSetup;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetId;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TargetedFieldAuthorizationTest.TargetedFieldController.class)
@DisplayName("Field authorization over the target of a record")
@CommonInfrastructureWebMvcSetup
class TargetedFieldAuthorizationTest {

    private static final String TARGET_ID_STRING = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String OTHER_ID_STRING = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final UUID TARGET_ID = UUID.fromString(TARGET_ID_STRING);
    private static final UUID OTHER_ID = UUID.fromString(OTHER_ID_STRING);

    @Autowired
    MockMvc mockMvc;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record TargetedResponse(
            @TargetId(TargetType.MEMBER) UUID id,
            String publicField,
            @HasAuthority({Authority.MEMBERS_MANAGE, Authority.EVENTS_REGISTRATIONS})
            String restrictedField,
            @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
            String ownerOrAdminField
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record CoordinatedResponse(
            @TargetId(TargetType.MEMBER) List<UUID> coordinators,
            @HasAuthority(Authority.EVENTS_REGISTRATIONS) @OwnerVisible
            String coordinatorsField
    ) {
    }

    record TargetedPatchRequest(
            JsonNullable<String> publicField,
            @HasAuthority({Authority.MEMBERS_MANAGE, Authority.EVENTS_REGISTRATIONS})
            JsonNullable<String> restrictedField
    ) {
    }

    @MvcComponent
    @RestController
    static class TargetedFieldController {

        @GetMapping(value = "/api/test/targeted/{id}", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<TargetedResponse> get(@PathVariable UUID id) {
            EntityModel<TargetedResponse> model = EntityModel.of(
                    new TargetedResponse(id, "public-value", "restricted-value", "owner-or-admin-value"));
            klabisLinkTo(methodOn(TargetedFieldController.class).get(id)).ifPresent(link ->
                    model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TargetedFieldController.class).update(id, null)))));
            return model;
        }

        @PatchMapping("/api/test/targeted/{id}")
        ResponseEntity<Void> update(@PathVariable @TargetId(TargetType.MEMBER) UUID id,
                                    @RequestBody TargetedPatchRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/api/test/coordinated", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<CoordinatedResponse> getCoordinated() {
            return EntityModel.of(new CoordinatedResponse(List.of(OTHER_ID, TARGET_ID), "coordinators-value"));
        }
    }

    @Nested
    @DisplayName("response field with several authorities")
    class ResponseField {

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("is visible with a grant of the second authority over the record's target")
        void visibleWithGrantOverTheRecordsTarget() throws Exception {
            mockMvc.perform(get("/api/test/targeted/" + TARGET_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.restrictedField").value("restricted-value"))
                    .andExpect(jsonPath("$.ownerOrAdminField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("is hidden on another record the user holds no grant over")
        void hiddenOnAnotherRecord() throws Exception {
            mockMvc.perform(get("/api/test/targeted/" + OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.restrictedField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
        @DisplayName("is visible on every record with a grant over everything")
        void visibleWithGrantOverEverything() throws Exception {
            mockMvc.perform(get("/api/test/targeted/" + OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.restrictedField").value("restricted-value"))
                    .andExpect(jsonPath("$.ownerOrAdminField").value("owner-or-admin-value"));
        }

        @Test
        @WithKlabisMockUser(memberId = TARGET_ID_STRING)
        @DisplayName("owner-visible field is visible on the user's own record only")
        void ownerVisibleOnOwnRecordOnly() throws Exception {
            mockMvc.perform(get("/api/test/targeted/" + TARGET_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.ownerOrAdminField").value("owner-or-admin-value"))
                    .andExpect(jsonPath("$.restrictedField").doesNotExist());
            mockMvc.perform(get("/api/test/targeted/" + OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.ownerOrAdminField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(memberId = TARGET_ID_STRING)
        @DisplayName("owner-visible field is visible when the user is any of the listed owners")
        void ownerVisibleWhenAnyOfOwnersIsTheUser() throws Exception {
            mockMvc.perform(get("/api/test/coordinated").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.coordinatorsField").value("coordinators-value"));
        }

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("a grant over one of several listed owners does not reveal the field")
        void grantOverOneOfOwnersDoesNotRevealField() throws Exception {
            mockMvc.perform(get("/api/test/coordinated").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.coordinatorsField").doesNotExist());
        }
    }

    @Nested
    @DisplayName("request field with several authorities")
    class RequestField {

        private static final String BODY = "{\"restrictedField\":\"new\"}";

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("is accepted with a grant over the path's target")
        void acceptedWithGrantOverTheTarget() throws Exception {
            mockMvc.perform(patch("/api/test/targeted/" + TARGET_ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("is rejected with a grant over another target")
        void rejectedWithGrantOverAnotherTarget() throws Exception {
            mockMvc.perform(patch("/api/test/targeted/" + OTHER_ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithKlabisMockUser
        @DisplayName("is rejected without any write authority, while unrestricted fields are accepted")
        void rejectedWithoutAuthority() throws Exception {
            mockMvc.perform(patch("/api/test/targeted/" + TARGET_ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/test/targeted/" + TARGET_ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"publicField\":\"new\"}"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("template property with several authorities")
    class TemplateProperty {

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("is offered for a grant over the target of the invocation")
        void offeredForGrantOverTheTarget() throws Exception {
            mockMvc.perform(get("/api/test/targeted/" + TARGET_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'restrictedField')]").exists());
        }

        @Test
        @WithKlabisMockUser(targetGrants = @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS,
                type = TargetType.MEMBER, ids = TARGET_ID_STRING))
        @DisplayName("is not offered for another target")
        void notOfferedForAnotherTarget() throws Exception {
            mockMvc.perform(get("/api/test/targeted/" + OTHER_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.update.properties[?(@.name == 'restrictedField')]").doesNotExist());
        }
    }
}
