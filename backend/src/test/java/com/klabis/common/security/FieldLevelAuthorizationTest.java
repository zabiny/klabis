package com.klabis.common.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationProxyFactory;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FieldLevelAuthorizationTest.TestController.class)
@Import({NullDeniedHandler.class, MaskDeniedHandler.class})
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

        private final AuthorizationProxyFactory proxyFactory;

        TestController(AuthorizationProxyFactory proxyFactory) {
            this.proxyFactory = proxyFactory;
        }

        @GetMapping("/test/field-auth")
        SensitiveDataView getSensitiveData() {
            SensitiveDataResponse response = new SensitiveDataResponse(
                    "public-value",
                    "secret-value",
                    "sensitive-value"
            );
            return (SensitiveDataView) proxyFactory.proxy(response);
        }
    }

    @Nested
    @DisplayName("authorized user with FIELD:READ authority")
    class AuthorizedUser {

        @Test
        @WithMockUser(authorities = FIELD_READ_AUTHORITY)
        @DisplayName("should see all fields with real values")
        void shouldSeeAllFieldsWithRealValues() throws Exception {
            mockMvc.perform(get("/test/field-auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").value("secret-value"))
                    .andExpect(jsonPath("$.maskedField").value("sensitive-value"));
        }
    }

    @Nested
    @DisplayName("user without FIELD:READ authority")
    class UnauthorizedUser {

        @Test
        @WithMockUser
        @DisplayName("hidden field should be absent from JSON response")
        void hiddenFieldShouldBeAbsentFromJson() throws Exception {
            mockMvc.perform(get("/test/field-auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.hiddenField").doesNotExist());
        }

        @Test
        @WithMockUser
        @DisplayName("masked field should contain mask value")
        void maskedFieldShouldContainMaskValue() throws Exception {
            mockMvc.perform(get("/test/field-auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicField").value("public-value"))
                    .andExpect(jsonPath("$.maskedField").value(MaskDeniedHandler.MASK_VALUE));
        }
    }
}
