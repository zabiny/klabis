package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
@WithPostprocessors
class FieldLevelAuthorizationTest {

    private static final String OWNER_ID_STRING = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String OTHER_ID_STRING = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final UUID OWNER_ID = UUID.fromString(OWNER_ID_STRING);
    private static final UUID OTHER_ID = UUID.fromString(OTHER_ID_STRING);

    private static final String FIELD_READ_AUTHORITY = "FIELD:READ";

    @Autowired
    MockMvc mockMvc;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record SensitiveDataResponse(
            String publicField,
            @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
            String hiddenField,
            @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
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

            @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
            JsonNullable<String> hiddenField,

            @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
            JsonNullable<String> maskedField,

            @HasAuthority(Authority.MEMBERS_MANAGE)
            JsonNullable<String> hasAuthorityHiddenField,

            @HasAuthority(Authority.MEMBERS_MANAGE)
            JsonNullable<String> hasAuthorityMaskedField
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    record OwnershipDataResponse(
            @OwnerId UUID ownerId,
            String publicField,
            @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible String ownerOrAdminField,
            @OwnerVisible String ownerOnlyField,
            @HasAuthority(Authority.MEMBERS_MANAGE) String adminOnlyField
    ) {}

    record OwnershipPatchRequest(
            JsonNullable<String> publicField,
            @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible JsonNullable<String> ownerOrAdminField,
            @OwnerVisible JsonNullable<String> ownerOnlyField
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

        @GetMapping(value = "/test/ownership-auth", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<OwnershipDataResponse> getOwnershipData() {
            return EntityModel.of(new OwnershipDataResponse(
                    OWNER_ID,
                    "public-value",
                    "owner-or-admin-value",
                    "owner-only-value",
                    "admin-only-value"
            ));
        }

        @PatchMapping("/api/test/ownership-auth/{id}")
        ResponseEntity<Void> updateOwnershipData(@PathVariable @OwnerId UUID id, @RequestBody OwnershipPatchRequest body) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/api/test/ownership-method/{id}")
        @com.klabis.common.users.HasAuthority(Authority.MEMBERS_MANAGE)
        @OwnerVisible
        ResponseEntity<String> getOwnershipProtectedResource(@PathVariable @OwnerId UUID id) {
            return ResponseEntity.ok("protected-data");
        }

        @GetMapping("/api/test/owner-only-method/{id}")
        @OwnerVisible
        ResponseEntity<String> getOwnerOnlyResource(@PathVariable @OwnerId UUID id) {
            return ResponseEntity.ok("owner-only-data");
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
    @DisplayName("authorized user with FIELD:READ authority")
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
        @DisplayName("PATCH with JsonNullable request should succeed when user has required authority from @PreAuthorize")
        void patchShouldSucceedWithRequiredAuthorityForPreAuthorize() throws Exception {
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
        @DisplayName("user with FIELD:READ sees hiddenField and maskedField but not hasAuthority* fields")
        void fieldReadUserSeesPreAuthorizeFieldsOnly() throws Exception {
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
        @DisplayName("PATCH with FIELD:READ sending authorized field and unauthorized hasAuthorityHiddenField returns 403")
        void patchWithMixedAuthoritiesReturns403() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hiddenField\": \"ok\", \"hasAuthorityHiddenField\": \"no-auth\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY})
        @DisplayName("HAL+FORMS template with FIELD:READ shows hiddenField and maskedField, hides hasAuthority* fields")
        void halFormsTemplateWithFieldReadShowsPreAuthorizeProperties() throws Exception {
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
    @DisplayName("user without FIELD:READ authority")
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
        @DisplayName("HAL+FORMS template should only contain publicField when user lacks FIELD:READ and MEMBERS:MANAGE authority")
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
        @DisplayName("PATCH with JsonNullable request should return 403 when user attempts to update field where he lacks required authority defined by @PreAuthorize")
        void patchShouldReturn403WithoutRequiredAuthorityPreAuthorize() throws Exception {
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
    @DisplayName("ownership-based field authorization")
    class OwnershipAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("admin (non-owner) sees all fields including ownerOrAdminField and adminOnlyField")
        void adminNonOwnerSeesAllFields() throws Exception {
            mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.ownerOrAdminField").value("owner-or-admin-value"))
                    .andExpect(jsonPath("$.adminOnlyField").value("admin-only-value"))
                    .andExpect(jsonPath("$.ownerOnlyField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("owner without admin authority sees ownerOrAdminField and ownerOnlyField but not adminOnlyField")
        void ownerWithoutAdminAuthoritySeesOwnerVisibleFields() throws Exception {
            mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.ownerOrAdminField").value("owner-or-admin-value"))
                    .andExpect(jsonPath("$.ownerOnlyField").value("owner-only-value"))
                    .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
        }

        @Test
        @WithMockUser
        @DisplayName("non-owner without authority sees only publicField")
        void nonOwnerWithoutAuthoritySeesOnlyPublicField() throws Exception {
            mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.ownerOrAdminField").doesNotExist())
                    .andExpect(jsonPath("$.ownerOnlyField").doesNotExist())
                    .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("owner does not see adminOnlyField (has only @HasAuthority, not @OwnerVisible)")
        void ownerDoesNotSeeAdminOnlyField() throws Exception {
            mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("non-owner with KlabisJwtAuthenticationToken does not see owner-only fields")
        void nonOwnerWithJwtTokenDoesNotSeeOwnerFields() throws Exception {
            mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.ownerOrAdminField").doesNotExist())
                    .andExpect(jsonPath("$.ownerOnlyField").doesNotExist())
                    .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
        }
    }

    @Nested
    @DisplayName("method-level ownership authorization")
    class MethodLevelOwnershipAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("admin (non-owner) can access @HasAuthority + @OwnerVisible method via authority")
        void adminWithoutOwnershipCanAccess() throws Exception {
            mockMvc.perform(get("/api/test/ownership-method/{id}", OTHER_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("owner without admin authority can access @HasAuthority + @OwnerVisible method via ownership")
        void ownerWithoutAuthorityCanAccess() throws Exception {
            mockMvc.perform(get("/api/test/ownership-method/{id}", OWNER_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("non-owner without authority gets 403 on @HasAuthority + @OwnerVisible method")
        void nonOwnerWithoutAuthorityGetsForbidden() throws Exception {
            mockMvc.perform(get("/api/test/ownership-method/{id}", OWNER_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("owner can access @OwnerVisible-only method")
        void ownerCanAccessOwnerOnlyMethod() throws Exception {
            mockMvc.perform(get("/api/test/owner-only-method/{id}", OWNER_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("non-owner gets 403 on @OwnerVisible-only method")
        void nonOwnerGetsForbiddenOnOwnerOnlyMethod() throws Exception {
            mockMvc.perform(get("/api/test/owner-only-method/{id}", OWNER_ID))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("ownership-based PATCH field authorization")
    class OwnershipPatchAuthorization {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("admin (non-owner) can PATCH ownerOrAdminField via authority")
        void adminCanPatchOwnerOrAdminField() throws Exception {
            mockMvc.perform(patch("/api/test/ownership-auth/{id}", OTHER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ownerOrAdminField\": \"new-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("owner can PATCH ownerOrAdminField and ownerOnlyField")
        void ownerCanPatchOwnerVisibleFields() throws Exception {
            mockMvc.perform(patch("/api/test/ownership-auth/{id}", OWNER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ownerOrAdminField\": \"new-value\", \"ownerOnlyField\": \"other-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("non-owner without authority gets 403 when PATCHing ownerOrAdminField")
        void nonOwnerWithoutAuthorityCannotPatchOwnerOrAdminField() throws Exception {
            mockMvc.perform(patch("/api/test/ownership-auth/{id}", OWNER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ownerOrAdminField\": \"new-value\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithKlabisMockUser(memberId = OTHER_ID_STRING)
        @DisplayName("non-owner without authority gets 403 when PATCHing ownerOnlyField")
        void nonOwnerWithoutAuthorityCannotPatchOwnerOnlyField() throws Exception {
            mockMvc.perform(patch("/api/test/ownership-auth/{id}", OWNER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ownerOnlyField\": \"new-value\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithKlabisMockUser(memberId = OWNER_ID_STRING)
        @DisplayName("owner can PATCH publicField without auth check")
        void ownerCanPatchPublicField() throws Exception {
            mockMvc.perform(patch("/api/test/ownership-auth/{id}", OWNER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithMockUser
        @DisplayName("non-owner can PATCH publicField without auth check")
        void nonOwnerCanPatchPublicField() throws Exception {
            mockMvc.perform(patch("/api/test/ownership-auth/{id}", OWNER_ID)
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
