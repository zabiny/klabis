package com.klabis.common.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.klabis.common.hateoas.HateoasConfiguration;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.AuthorizationAdvisorProxyFactory;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FieldLevelAuthorizationTest.TestController.class)
@Import({SecurityConfiguration.class, NullDeniedHandler.class, MaskDeniedHandler.class, HateoasConfiguration.class})
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
    }

    record SensitiveDataResponse(
            String publicField,
            String hiddenField,
            String maskedField
    ) implements SensitiveDataView {
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
                    "sensitive-value"
            );
            SensitiveDataView proxied = (SensitiveDataView) proxyFactory.proxy(response);
            return EntityModel.of(proxied)
                    .add(klabisLinkTo(methodOn(TestController.class).getSensitiveData()).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TestController.class).updateSensitiveData(null))));
        }

        @PatchMapping(value = "/test/field-auth")
        org.springframework.http.ResponseEntity<Void> updateSensitiveData(@RequestBody SensitiveDataResponse body) {
            return org.springframework.http.ResponseEntity.noContent().build();
        }
    }

    @Nested
    @DisplayName("authorized user with FIELD:READ authority")
    class AuthorizedUser {

        @Test
        @WithMockUser(authorities = FIELD_READ_AUTHORITY)
        @DisplayName("should see all fields with real values in JSON response")
        void shouldSeeAllFieldsWithRealValues() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").value("secret-value"))
                    .andExpect(jsonPath("$.maskedField").value("sensitive-value"));
        }

        @Test
        @WithMockUser(authorities = FIELD_READ_AUTHORITY)
        @DisplayName("should see all properties in HAL+FORMS PATCH template")
        void shouldSeeAllPropertiesInHalFormsTemplate() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hiddenField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'maskedField')]").exists());
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
                    .andExpect(jsonPath("$.maskedField").value(MaskDeniedHandler.MASK_VALUE));
        }

        @Test
        @WithMockUser
        @DisplayName("HAL+FORMS template should only contain publicField when user lacks FIELD:READ authority")
        void shouldFilterTemplatePropertiesBasedOnAuthorization() throws Exception {
            mockMvc.perform(get("/test/field-auth").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'publicField')]").exists())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'hiddenField')]").doesNotExist())
                    .andExpect(jsonPath("$._templates.default.properties[?(@.name == 'maskedField')]").doesNotExist());
        }
    }
}
