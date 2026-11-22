package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetId;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.CommonInfrastructureWebMvcSetup;
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
import org.springframework.security.test.context.support.WithMockUser;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FieldLevelAuthorizationTest.TestController.class)
@DisplayName("Field-level authorization on response DTOs")
@CommonInfrastructureWebMvcSetup
class FieldLevelAuthorizationTest {

    private static final String SELF_ID_STRING = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String OTHER_ID_STRING = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final UUID SELF_ID = UUID.fromString(SELF_ID_STRING);
    private static final UUID OTHER_ID = UUID.fromString(OTHER_ID_STRING);

    private static final String FIELD_READ_AUTHORITY = "MEMBERS:READ";

    @Autowired
    MockMvc mockMvc;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record SensitiveDataResponse(
            String publicField,
            @HasAuthority(Authority.MEMBERS_READ)
            String hiddenField,
            @HasAuthority(Authority.MEMBERS_READ)
            @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
            String maskedField,
            @HasAuthority(Authority.MEMBERS_MANAGE)
            String hasAuthorityHiddenField,
            @HasAuthority(Authority.MEMBERS_MANAGE)
            @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
            String hasAuthorityMaskedField
    ) {
    }

    record PatchSensitiveDataRequest(
            JsonNullable<String> publicField,

            @HasAuthority(Authority.MEMBERS_READ)
            JsonNullable<String> hiddenField,

            @HasAuthority(Authority.MEMBERS_READ)
            JsonNullable<String> maskedField,

            @HasAuthority(Authority.MEMBERS_MANAGE)
            JsonNullable<String> hasAuthorityHiddenField,

            @HasAuthority(Authority.MEMBERS_MANAGE)
            JsonNullable<String> hasAuthorityMaskedField
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record SelfGrantDataResponse(
            @TargetId(TargetType.MEMBER) UUID selfId,
            String publicField,
            @HasAuthority({Authority.MEMBERS_MANAGE, Authority.MEMBERS_EDIT_PROFILE}) String guardedField
    ) {}

    record SelfGrantPatchRequest(
            JsonNullable<String> publicField,
            @HasAuthority({Authority.MEMBERS_MANAGE, Authority.MEMBERS_EDIT_PROFILE}) JsonNullable<String> guardedField
    ) {}

    // JsonNullable<T> response fields go through a second ValueSerializerModifier
    // (org.openapitools:jackson-databind-nullable's JsonNullableJackson3Module), registered
    // independently of FieldSecurityJacksonModule with no defined order between the two. This
    // record exists to prove @HasAuthority still gates a JsonNullable-typed property despite that.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record JsonNullableFieldResponse(
            String publicField,
            @HasAuthority(Authority.MEMBERS_MANAGE)
            JsonNullable<String> hasAuthorityJsonNullableField
    ) {}

    // No @JsonInclude(NON_NULL) here on purpose: under NON_NULL, JsonNullable.undefined() and
    // JsonNullable.of(null) can both end up omitted, which would hide the distinction this test
    // record exists to observe — a defined-but-null value must still serialize as an explicit
    // `null`, not be silently dropped like the undefined case.
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record JsonNullableDefinedNullFieldResponse(
            String publicField,
            @HasAuthority(Authority.MEMBERS_MANAGE)
            JsonNullable<String> hasAuthorityJsonNullableField
    ) {}

    @MvcComponent
    @RestController
    static class TestController {

        @GetMapping(value = "/test/field-auth", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<SensitiveDataResponse> getSensitiveData() {
            SensitiveDataResponse response = new SensitiveDataResponse(
                    "public-value",
                    "secret-value",
                    "sensitive-value",
                    "authority-secret-value",
                    "authority-sensitive-value"
            );
            EntityModel<SensitiveDataResponse> model = EntityModel.of(response);
            klabisLinkTo(methodOn(TestController.class).getSensitiveData()).ifPresent(link ->
                    model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TestController.class).updateSensitiveData(null)))));
            return model;
        }

        @PatchMapping(value = "/test/field-auth")
        ResponseEntity<Void> updateSensitiveData(@RequestBody PatchSensitiveDataRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/test/field-auth-nulls", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<SensitiveDataResponse> getSensitiveDataWithNulls() {
            return EntityModel.of(new SensitiveDataResponse("public-value", null, "sensitive-value", "authority-secret-value", "authority-sensitive-value"));
        }

        @GetMapping(value = "/test/self-grant-auth", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<SelfGrantDataResponse> getSelfGrantData() {
            return EntityModel.of(new SelfGrantDataResponse(
                    SELF_ID,
                    "public-value",
                    "guarded-value"
            ));
        }

        @PatchMapping("/api/test/self-grant-auth/{id}")
        ResponseEntity<Void> updateSelfGrantData(@PathVariable @TargetId(TargetType.MEMBER) UUID id, @RequestBody SelfGrantPatchRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/api/test/target-method/{id}")
        @com.klabis.common.users.HasAuthority({Authority.MEMBERS_MANAGE, Authority.MEMBERS_EDIT_PROFILE})
        ResponseEntity<String> getTargetProtectedResource(@PathVariable @TargetId(TargetType.MEMBER) UUID id) {
            return ResponseEntity.ok("protected-data");
        }

        @GetMapping(value = "/test/json-nullable-field-auth", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<JsonNullableFieldResponse> getJsonNullableFieldData() {
            return EntityModel.of(new JsonNullableFieldResponse(
                    "public-value", JsonNullable.of("secret-value")));
        }

        @GetMapping(value = "/test/json-nullable-field-auth-undefined", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<JsonNullableFieldResponse> getJsonNullableFieldDataUndefined() {
            return EntityModel.of(new JsonNullableFieldResponse(
                    "public-value", JsonNullable.undefined()));
        }

        @GetMapping(value = "/test/json-nullable-field-auth-defined-null", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<JsonNullableDefinedNullFieldResponse> getJsonNullableFieldDataDefinedNull() {
            return EntityModel.of(new JsonNullableDefinedNullFieldResponse(
                    "public-value", JsonNullable.of(null)));
        }
    }

    @Nested
    @DisplayName("authorized user with MEMBERS:READ authority")
    class AuthorizedUser {

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY, "MEMBERS:MANAGE"})
        @DisplayName("should see all fields with real values in JSON response")
        void shouldSeeAllFieldsWithRealValues() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").value("secret-value"))
                    .andExpect(jsonPath("$.maskedField").value("sensitive-value"))
                    .andExpect(jsonPath("$.hasAuthorityHiddenField").value("authority-secret-value"))
                    .andExpect(jsonPath("$.hasAuthorityMaskedField").value("authority-sensitive-value"));
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY, "MEMBERS:MANAGE"})
        @DisplayName("should see all properties in HAL+FORMS PATCH template")
        void shouldSeeAllPropertiesInHalFormsTemplate() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'maskedField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityHiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityMaskedField')]").exists());
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY})
        @DisplayName("PATCH with JsonNullable request should succeed when user has required authority from @HasAuthority")
        void patchShouldSucceedWithRequiredAuthorityForMembersRead() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hiddenField\": \"new-secret\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("PATCH with JsonNullable request should return 2XX when user has required authority from @HasAuthority")
        void patchShouldSucceedWithRequiredAuthorityForHasAuthority() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hasAuthorityHiddenField\": \"new-secret\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY, "MEMBERS:MANAGE"})
        @DisplayName("null-valued secured field is omitted by @JsonInclude(NON_NULL), not by authorization")
        void nullValuedSecuredFieldIsOmittedByJsonIncludeNotByAuthorization() throws Exception {
            mockMvc.perform(get("/test/field-auth-nulls").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").doesNotExist())
                    .andExpect(jsonPath("$.maskedField").value("sensitive-value"))
                    .andExpect(jsonPath("$.hasAuthorityHiddenField").value("authority-secret-value"))
                    .andExpect(jsonPath("$.hasAuthorityMaskedField").value("authority-sensitive-value"));
        }

    }

    @Nested
    @DisplayName("user with partial authorities")
    class PartialAuthorityUser {

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY})
        @DisplayName("user with MEMBERS:READ sees hiddenField and maskedField but not hasAuthority* fields")
        void fieldReadUserSeesMembersReadFieldsOnly() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").value("secret-value"))
                    .andExpect(jsonPath("$.maskedField").value("sensitive-value"))
                    .andExpect(jsonPath("$.hasAuthorityHiddenField").doesNotExist())
                    .andExpect(jsonPath("$.hasAuthorityMaskedField").value(MaskDeniedHandler.MASK_VALUE));
        }

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("user with MEMBERS:MANAGE sees hasAuthority* fields but hiddenField absent and maskedField masked")
        void membersManageUserSeesHasAuthorityFieldsOnly() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").doesNotExist())
                    .andExpect(jsonPath("$.maskedField").value(MaskDeniedHandler.MASK_VALUE))
                    .andExpect(jsonPath("$.hasAuthorityHiddenField").value("authority-secret-value"))
                    .andExpect(jsonPath("$.hasAuthorityMaskedField").value("authority-sensitive-value"));
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY})
        @DisplayName("PATCH with MEMBERS:READ sending authorized field and unauthorized hasAuthorityHiddenField returns 403")
        void patchWithMixedAuthoritiesReturns403() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hiddenField\": \"ok\", \"hasAuthorityHiddenField\": \"no-auth\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/errors/field-authorization"))
                    .andExpect(jsonPath("$.field").value("hasAuthorityHiddenField"));
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY})
        @DisplayName("HAL+FORMS template with MEMBERS:READ shows hiddenField and maskedField, hides hasAuthority* fields")
        void halFormsTemplateWithFieldReadShowsMembersReadProperties() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'maskedField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityHiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityMaskedField')]").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("HAL+FORMS template with MEMBERS:MANAGE shows hasAuthority* fields, hides hiddenField and maskedField")
        void halFormsTemplateWithMembersManageShowsHasAuthorityProperties() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'maskedField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityHiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityMaskedField')]").exists());
        }
    }

    @Nested
    @DisplayName("user without MEMBERS:READ authority")
    class UnauthorizedUser {

        @Test
        @WithMockUser
        @DisplayName("hidden field absent, masked field shows mask, public field visible in JSON response")
        void shouldFilterFieldsBasedOnAuthorization() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").doesNotExist())
                    .andExpect(jsonPath("$.maskedField").value(MaskDeniedHandler.MASK_VALUE))
                    .andExpect(jsonPath("$.hasAuthorityHiddenField").doesNotExist())
                    .andExpect(jsonPath("$.hasAuthorityMaskedField").value(MaskDeniedHandler.MASK_VALUE));
        }

        @Test
        @WithMockUser
        @DisplayName("HAL+FORMS template should only contain publicField when user lacks MEMBERS:READ and MEMBERS:MANAGE authority")
        void shouldFilterTemplatePropertiesBasedOnAuthorization() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'maskedField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityHiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.updateSensitiveData.properties[?(@.name == 'hasAuthorityMaskedField')]").doesNotExist());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with JsonNullable request should return 403 when user attempts to update field where he lacks required authority defined by @HasAuthority")
        void patchShouldReturn403WithoutRequiredAuthority() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hiddenField\": \"new-secret\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with JsonNullable request should return 403 when user attempts to update field where he lacks required authority defined by @HasAuthority")
        void patchShouldReturn403WithoutRequiredAuthorityHasAuthority() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hasAuthorityHiddenField\": \"new-secret\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with JsonNullable request should return 2XX when user update only public fields")
        void patchShouldReturn200IfAllUpdatedFieldsAreAvailableForUser() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithMockUser
        @DisplayName("null-valued secured field is absent (not masked) even without authorization")
        void nullValuedSecuredFieldIsAbsentNotMaskedWithoutAuthorization() throws Exception {
            mockMvc.perform(get("/test/field-auth-nulls").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").doesNotExist())
                    .andExpect(jsonPath("$.maskedField").value(MaskDeniedHandler.MASK_VALUE))
                    .andExpect(jsonPath("$.hasAuthorityHiddenField").doesNotExist())
                    .andExpect(jsonPath("$.hasAuthorityMaskedField").value(MaskDeniedHandler.MASK_VALUE));
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with empty body succeeds when no secured JsonNullable is provided")
        void patchWithEmptyBodySucceeds() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with explicit null for secured field returns 403 because JsonNullable.of(null) is still provided")
        void patchWithExplicitNullForSecuredFieldReturns403() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hasAuthorityHiddenField\": null}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("field authorization of a user over their own record")
    class SelfGrantAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("admin (no grant over themself) sees guardedField")
        void adminSeesGuardedField() throws Exception {
            mockMvc.perform(get("/test/self-grant-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.guardedField").value("guarded-value"));
        }

        @Test
        @WithKlabisMockUser(memberId = SELF_ID_STRING, targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE,
                type = TargetType.MEMBER, ids = SELF_ID_STRING))
        @DisplayName("user with a grant over themself sees guardedField on their own record")
        void userWithSelfGrantSeesGuardedField() throws Exception {
            mockMvc.perform(get("/test/self-grant-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.guardedField").value("guarded-value"));
        }

        @Test
        @WithKlabisMockUser(memberId = SELF_ID_STRING)
        @DisplayName("user without a grant over themself sees only publicField")
        void userWithoutGrantSeesOnlyPublicField() throws Exception {
            mockMvc.perform(get("/test/self-grant-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.guardedField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING, targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE,
                type = TargetType.MEMBER, ids = OTHER_ID_STRING))
        @DisplayName("user with a grant over themself does not see guardedField on someone else's record")
        void selfGrantDoesNotRevealOtherRecord() throws Exception {
            mockMvc.perform(get("/test/self-grant-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.guardedField").doesNotExist());
        }

        @Test
        @WithMockUser
        @DisplayName("user without authority sees only publicField")
        void userWithoutAuthoritySeesOnlyPublicField() throws Exception {
            mockMvc.perform(get("/test/self-grant-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.guardedField").doesNotExist());
        }
    }

    @Nested
    @DisplayName("method-level authorization over the target")
    class MethodLevelTargetAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("admin can access the method via authority over everything")
        void adminCanAccess() throws Exception {
            mockMvc.perform(get("/api/test/target-method/{id}", OTHER_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithKlabisMockUser(memberId = SELF_ID_STRING, targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE,
                type = TargetType.MEMBER, ids = SELF_ID_STRING))
        @DisplayName("user with a grant over themself can access the method for themself")
        void userWithSelfGrantCanAccessOwnTarget() throws Exception {
            mockMvc.perform(get("/api/test/target-method/{id}", SELF_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithKlabisMockUser(memberId = SELF_ID_STRING, targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE,
                type = TargetType.MEMBER, ids = SELF_ID_STRING))
        @DisplayName("user with a grant over themself gets 403 for someone else")
        void userWithSelfGrantForbiddenForOtherTarget() throws Exception {
            mockMvc.perform(get("/api/test/target-method/{id}", OTHER_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("user without authority gets 403")
        void userWithoutAuthorityGetsForbidden() throws Exception {
            mockMvc.perform(get("/api/test/target-method/{id}", SELF_ID))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH field authorization over the target")
    class SelfGrantPatchAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("admin can PATCH guardedField via authority")
        void adminCanPatchGuardedField() throws Exception {
            mockMvc.perform(patch("/api/test/self-grant-auth/{id}", OTHER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"guardedField\": \"new-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithKlabisMockUser(memberId = SELF_ID_STRING, targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE,
                type = TargetType.MEMBER, ids = SELF_ID_STRING))
        @DisplayName("user with a grant over themself can PATCH guardedField on their own record")
        void userWithSelfGrantCanPatchOwnRecord() throws Exception {
            mockMvc.perform(patch("/api/test/self-grant-auth/{id}", SELF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"guardedField\": \"new-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithKlabisMockUser(memberId = SELF_ID_STRING, targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE,
                type = TargetType.MEMBER, ids = SELF_ID_STRING))
        @DisplayName("user with a grant over themself gets 403 when PATCHing guardedField of someone else")
        void userWithSelfGrantCannotPatchOtherRecord() throws Exception {
            mockMvc.perform(patch("/api/test/self-grant-auth/{id}", OTHER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"guardedField\": \"new-value\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("user without authority gets 403 when PATCHing guardedField")
        void userWithoutAuthorityCannotPatchGuardedField() throws Exception {
            mockMvc.perform(patch("/api/test/self-grant-auth/{id}", SELF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"guardedField\": \"new-value\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("any user can PATCH publicField without auth check")
        void anyUserCanPatchPublicField() throws Exception {
            mockMvc.perform(patch("/api/test/self-grant-auth/{id}", SELF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Nested
    @DisplayName("@HasAuthority on a JsonNullable<T> response field")
    class JsonNullableFieldAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("authorized user sees the JsonNullable field's value")
        void authorizedUserSeesJsonNullableField() throws Exception {
            mockMvc.perform(get("/test/json-nullable-field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hasAuthorityJsonNullableField").value("secret-value"));
        }

        @Test
        @WithMockUser
        @DisplayName("unauthorized user does not see the JsonNullable field, even though it is present")
        void unauthorizedUserDoesNotSeeJsonNullableField() throws Exception {
            mockMvc.perform(get("/test/json-nullable-field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hasAuthorityJsonNullableField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("undefined JsonNullable still serializes as absent for an authorized caller")
        void authorizedUserStillSeesUndefinedAsAbsent() throws Exception {
            // Guards against a fix that "hides" a field by making SecuredBeanPropertyWriter
            // always write null instead of delegating to the JsonNullable writer's own
            // undefined-skip logic — that would satisfy the two tests above while silently
            // breaking JsonNullable's tri-state contract for authorized callers.
            mockMvc.perform(get("/test/json-nullable-field-auth-undefined").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hasAuthorityJsonNullableField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("a defined-but-null JsonNullable still serializes as explicit null for an authorized caller")
        void authorizedUserSeesDefinedNullAsExplicitNull() throws Exception {
            // The third leg of the tri-state: defined-null must be distinguishable from
            // undefined. If the security wrapper collapsed both to "absent", this would pass
            // the undefined-case test above for the wrong reason.
            mockMvc.perform(get("/test/json-nullable-field-auth-defined-null").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hasAuthorityJsonNullableField").value(org.hamcrest.Matchers.nullValue()));
        }
    }
}
