package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.patch.PatchField;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserService;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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

@WebMvcTest(controllers = FieldLevelAuthorizationTest.TestController.class)
@DisplayName("Field-level authorization on response DTOs")
class FieldLevelAuthorizationTest {

    private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private static final String FIELD_READ_AUTHORITY = "FIELD:READ";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    UserDetailsService userDetailsService;

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
            PatchField<String> publicField,

            @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
            PatchField<String> hiddenField,

            @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
            PatchField<String> maskedField,

            @HasAuthority(Authority.MEMBERS_MANAGE)
            PatchField<String> hasAuthorityHiddenField,

            @HasAuthority(Authority.MEMBERS_MANAGE)
            PatchField<String> hasAuthorityMaskedField
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
            return EntityModel.of(response)
                    .add(klabisLinkTo(methodOn(TestController.class).getSensitiveData()).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TestController.class).updateSensitiveData(null))));
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
    }

    private static void withOwnerAuthentication(UUID memberIdUuid, Authority... authorities) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", JwsAlgorithms.RS256)
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        List<SimpleGrantedAuthority> grantedAuthorities = java.util.Arrays.stream(authorities)
                .map(a -> new SimpleGrantedAuthority(a.getValue()))
                .toList();
        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(
                jwt, new UserId(memberIdUuid), memberIdUuid, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(token);
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
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'maskedField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityHiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityMaskedField')]").exists());
        }

        @Test
        @WithMockUser(authorities = {FIELD_READ_AUTHORITY})
        @DisplayName("PATCH with PatchField request should succeed when user has required authority from @PreAuthorize")
        void patchShouldSucceedWithRequiredAuthorityForPreAuthorize() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hiddenField\": \"new-secret\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithMockUser(authorities = {"MEMBERS:MANAGE"})
        @DisplayName("PATCH with PatchField request should return 2XX when user has required authority from @HasAuthority")
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
        @WithMockUser(authorities = {"MEMBERS:MANAGE"})
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
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'maskedField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityHiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityMaskedField')]").doesNotExist());
        }

        @Test
        @WithMockUser(authorities = {"MEMBERS:MANAGE"})
        @DisplayName("HAL+FORMS template with MEMBERS:MANAGE shows hasAuthority* fields, hides hiddenField and maskedField")
        void halFormsTemplateWithMembersManageShowsHasAuthorityProperties() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'maskedField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityHiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityMaskedField')]").exists());
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
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'maskedField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityHiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hasAuthorityMaskedField')]").doesNotExist());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with PatchField request should return 403 when user attempts to update field where he lacks required authority defined by @PreAuthorize")
        void patchShouldReturn403WithoutRequiredAuthorityPreAuthorize() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hiddenField\": \"new-secret\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with PatchField request should return 403 when user attempts to update field where he lacks required authority defined by @HasAuthority")
        void patchShouldReturn403WithoutRequiredAuthorityHasAuthority() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"publicField\": \"new-value\", \"hasAuthorityHiddenField\": \"new-secret\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with PatchField request should return 2XX when user update only public fields")
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
        @DisplayName("PATCH with empty body succeeds when no secured PatchField is provided")
        void patchWithEmptyBodySucceeds() throws Exception {
            mockMvc.perform(patch("/test/field-auth")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH with explicit null for secured field returns 403 because PatchField.of(null) is still provided")
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
        @WithMockUser(authorities = {"MEMBERS:MANAGE"})
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
        @DisplayName("owner without admin authority sees ownerOrAdminField and ownerOnlyField but not adminOnlyField")
        void ownerWithoutAdminAuthoritySeesOwnerVisibleFields() throws Exception {
            withOwnerAuthentication(OWNER_ID);
            try {
                mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.publicField").value("public-value"))
                        .andExpect(jsonPath("$.ownerOrAdminField").value("owner-or-admin-value"))
                        .andExpect(jsonPath("$.ownerOnlyField").value("owner-only-value"))
                        .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
            } finally {
                SecurityContextHolder.clearContext();
            }
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
        @DisplayName("owner does not see adminOnlyField (has only @HasAuthority, not @OwnerVisible)")
        void ownerDoesNotSeeAdminOnlyField() throws Exception {
            withOwnerAuthentication(OWNER_ID);
            try {
                mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        @Test
        @DisplayName("non-owner with KlabisJwtAuthenticationToken does not see owner-only fields")
        void nonOwnerWithJwtTokenDoesNotSeeOwnerFields() throws Exception {
            withOwnerAuthentication(OTHER_ID);
            try {
                mockMvc.perform(get("/test/ownership-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.publicField").value("public-value"))
                        .andExpect(jsonPath("$.ownerOrAdminField").doesNotExist())
                        .andExpect(jsonPath("$.ownerOnlyField").doesNotExist())
                        .andExpect(jsonPath("$.adminOnlyField").doesNotExist());
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
