package com.klabis.common.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.patch.PatchField;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserService;
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
import org.springframework.security.authorization.method.AuthorizationAdvisorProxyFactory;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    private static final String FIELD_READ_AUTHORITY = "FIELD:READ";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    UserDetailsService userDetailsService;

    // --- Test DTO design ---
    // SensitiveDataView is the interface carrying the @PreAuthorize annotations.
    // SensitiveDataResponse record implements it — AuthorizationAdvisorProxyFactory
    // creates a JDK proxy via this interface (records are final, CGLIB cannot subclass them).

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
    interface SensitiveDataView {

        @JsonProperty
        String publicField();

        @JsonProperty
        @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
        String hiddenField();

        @JsonProperty
        @PreAuthorize("hasAuthority('" + FIELD_READ_AUTHORITY + "')")
        @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
        String maskedField();

        @JsonProperty
        @HasAuthority(Authority.MEMBERS_MANAGE)
        String hasAuthorityHiddenField();

        @JsonProperty
        @HasAuthority(Authority.MEMBERS_MANAGE)
        @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
        String hasAuthorityMaskedField();
    }

    record SensitiveDataResponse(
            String publicField,
            String hiddenField,
            String maskedField,
            String hasAuthorityHiddenField,
            String hasAuthorityMaskedField
    ) implements SensitiveDataView {
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

    @MvcComponent
    @RestController
    static class TestController {

        private final AuthorizationAdvisorProxyFactory proxyFactory;

        TestController(AuthorizationAdvisorProxyFactory proxyFactory) {
            this.proxyFactory = proxyFactory;
        }

        @GetMapping(value = "/test/field-auth", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        EntityModel<SensitiveDataView> getSensitiveData() {
            SensitiveDataResponse response = new SensitiveDataResponse(
                    "public-value",
                    "secret-value",
                    "sensitive-value",
                    "authority-secret-value",
                    "authority-sensitive-value"
            );
            SensitiveDataView proxied = (SensitiveDataView) proxyFactory.proxy(response);
            return EntityModel.of(proxied)
                    .add(klabisLinkTo(methodOn(TestController.class).getSensitiveData()).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TestController.class).updateSensitiveData(null))));
        }

        @PatchMapping(value = "/test/field-auth")
        ResponseEntity<Void> updateSensitiveData(@RequestBody PatchSensitiveDataRequest body) {
            return ResponseEntity.noContent().build();
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
    }
}
