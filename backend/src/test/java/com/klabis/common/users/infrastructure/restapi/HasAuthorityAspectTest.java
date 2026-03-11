package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = HasAuthorityAspectTest.HasAuthorityAspectTestConfiguration.class)
@DisplayName("HasAuthorityAspect Authorization Tests")
@ActiveProfiles("test")
class HasAuthorityAspectTest {

    @Autowired
    private TestService testService;

    @Autowired
    private ClassLevelAuthorizedService classLevelService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Method-level authorization checks")
    class MethodLevelAuthorization {

        @Test
        @DisplayName("should allow access when user has required authority")
        void shouldAllowAccessWithRequiredAuthority() {
            // Given
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            String result = testService.methodWithAuthority();
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("should deny access when user lacks required authority")
        void shouldDenyAccessWithoutRequiredAuthority() {
            // Given
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            assertThatThrownBy(() -> testService.methodWithAuthority())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied")
                    .hasMessageContaining(Authority.MEMBERS_MANAGE.getValue());
        }

        @Test
        @DisplayName("should deny access when user is not authenticated")
        void shouldDenyAccessWhenNotAuthenticated() {
            // Given
            SecurityContextHolder.clearContext();

            // When & Then
            assertThatThrownBy(() -> testService.methodWithAuthority())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("should allow access when user has multiple authorities including required")
        void shouldAllowAccessWithMultipleAuthoritiesIncludingRequired() {
            // Given
            Authentication auth = createAuthentication(
                    "user1",
                    Authority.MEMBERS_READ.getValue(),
                    Authority.MEMBERS_MANAGE.getValue(),
                    Authority.MEMBERS_PERMISSIONS.getValue()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            String result = testService.methodWithAuthority();
            assertThat(result).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("Method-level annotation takes precedence over class-level")
    class MethodLevelPrecedence {

        @Test
        @DisplayName("should use method-level authority when both are present")
        void shouldUseMethodLevelAuthorityWhenBothPresent() {
            // Given - user has MEMBERS_READ (class-level requirement)
            // but calling method that requires MEMBERS_MANAGE (method-level)
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then - should fail because method requires MEMBERS_MANAGE
            assertThatThrownBy(() -> testService.methodOverridingClassAuthority())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(Authority.MEMBERS_MANAGE.getValue());
        }

        @Test
        @DisplayName("should pass when method-level authority requirement is met")
        void shouldPassWhenMethodLevelAuthorityRequirementIsMet() {
            // Given - user has MEMBERS_MANAGE (method-level requirement)
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            String result = testService.methodOverridingClassAuthority();
            assertThat(result).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("Class-level authorization checks")
    class ClassLevelAuthorization {

        @Test
        @DisplayName("should allow access when user has class-level authority")
        void shouldAllowAccessWithClassLevelAuthority() {
            // Given
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            String result = classLevelService.classLevelMethod();
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("should deny access when user lacks class-level authority")
        void shouldDenyAccessWithoutClassLevelAuthority() {
            // Given
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            assertThatThrownBy(() -> classLevelService.classLevelMethod())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(Authority.MEMBERS_READ.getValue());
        }
    }

    /**
     * Helper method to create an authentication token with given authorities.
     */
    private static Authentication createAuthentication(String username, String... authorities) {
        Collection<GrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .map(auth -> (GrantedAuthority) auth)
                .toList();

        return new TestingAuthenticationToken(username, "password", grantedAuthorities);
    }

    /**
     * Test service class with @HasAuthority annotations.
     */
    @Service
    public static class TestService {

        @HasAuthority(Authority.MEMBERS_MANAGE)
        public String methodWithAuthority() {
            return "success";
        }

        @HasAuthority(Authority.MEMBERS_MANAGE)
        public String methodOverridingClassAuthority() {
            return "success";
        }
    }

    /**
     * Test class with @HasAuthority at class level.
     */
    @HasAuthority(Authority.MEMBERS_READ)
    @Service
    public static class ClassLevelAuthorizedService {
        public String classLevelMethod() {
            return "success";
        }
    }

    /**
     * Spring configuration for test.
     */
    @Configuration
    @EnableAspectJAutoProxy
    static class HasAuthorityAspectTestConfiguration {

        @Bean
        public HasAuthorityAspect hasAuthorityAspect() {
            return new HasAuthorityAspect();
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }

        @Bean
        public ClassLevelAuthorizedService classLevelAuthorizedService() {
            return new ClassLevelAuthorizedService();
        }
    }
}
