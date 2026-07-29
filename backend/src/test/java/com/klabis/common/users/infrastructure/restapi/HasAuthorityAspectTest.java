package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.security.HasAuthorityMethodInterceptor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

@SpringBootTest(classes = HasAuthorityAspectTest.HasAuthorityInterceptorTestConfiguration.class)
@DisplayName("HasAuthorityMethodInterceptor Authorization Tests")
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
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            String result = testService.methodWithAuthority();
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("should deny access when user lacks required authority")
        void shouldDenyAccessWithoutRequiredAuthority() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() -> testService.methodWithAuthority())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied")
                    .hasMessageContaining(Authority.MEMBERS_MANAGE.getValue());
        }

        @Test
        @DisplayName("should deny access when user is not authenticated")
        void shouldDenyAccessWhenNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> testService.methodWithAuthority())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("should allow access when user has multiple authorities including required")
        void shouldAllowAccessWithMultipleAuthoritiesIncludingRequired() {
            Authentication auth = createAuthentication(
                    "user1",
                    Authority.MEMBERS_READ.getValue(),
                    Authority.MEMBERS_MANAGE.getValue(),
                    Authority.MEMBERS_PERMISSIONS.getValue()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

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
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() -> testService.methodOverridingClassAuthority())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(Authority.MEMBERS_MANAGE.getValue());
        }

        @Test
        @DisplayName("should pass when method-level authority requirement is met")
        void shouldPassWhenMethodLevelAuthorityRequirementIsMet() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

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
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            String result = classLevelService.classLevelMethod();
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("should deny access when user lacks class-level authority")
        void shouldDenyAccessWithoutClassLevelAuthority() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() -> classLevelService.classLevelMethod())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(Authority.MEMBERS_READ.getValue());
        }
    }

    @Nested
    @DisplayName("Interface-level authorization checks")
    class InterfaceLevelAuthorization {

        @Autowired
        private InterfaceImplementingService interfaceImplementingService;

        @Test
        @DisplayName("should deny access when @HasAuthority is declared only on the implemented interface method")
        void shouldDenyAccessWhenInterfaceMethodAuthorityMissing() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_READ.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(interfaceImplementingService::interfaceMethodWithAuthority)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(Authority.MEMBERS_MANAGE.getValue());
        }

        @Test
        @DisplayName("should allow access when user has the authority declared on the implemented interface method")
        void shouldAllowAccessWhenInterfaceMethodAuthorityPresent() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            String result = interfaceImplementingService.interfaceMethodWithAuthority();
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("concrete class method annotation still takes priority over interface method annotation")
        void shouldPreferConcreteClassAnnotationOverInterface() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // interface requires MEMBERS_READ, concrete class overrides with MEMBERS_MANAGE
            String result = interfaceImplementingService.methodOverridingInterfaceAuthority();
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("should deny access when @HasAuthority is declared on the implemented interface type (class-level)")
        void shouldDenyAccessWhenInterfaceClassLevelAuthorityMissing() {
            Authentication auth = createAuthentication("user1", Authority.MEMBERS_MANAGE.getValue());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(interfaceImplementingService::classLevelInterfaceMethod)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(Authority.MEMBERS_READ.getValue());
        }
    }

    private static Authentication createAuthentication(String username, String... authorities) {
        Collection<GrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .map(auth -> (GrantedAuthority) auth)
                .toList();

        return new TestingAuthenticationToken(username, "password", grantedAuthorities);
    }

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

    @HasAuthority(Authority.MEMBERS_READ)
    @Service
    public static class ClassLevelAuthorizedService {
        public String classLevelMethod() {
            return "success";
        }
    }

    /**
     * Mirrors a generated OpenAPI {@code *Api} interface: authorization annotations live on the
     * interface method/type, and the concrete implementation carries none of its own (except where
     * explicitly overridden).
     */
    public interface InterfaceWithAuthority {

        @HasAuthority(Authority.MEMBERS_MANAGE)
        String interfaceMethodWithAuthority();

        @HasAuthority(Authority.MEMBERS_READ)
        String methodOverridingInterfaceAuthority();
    }

    @HasAuthority(Authority.MEMBERS_READ)
    public interface ClassLevelAuthorityInterface {
        String classLevelInterfaceMethod();
    }

    @Service
    public static class InterfaceImplementingService implements InterfaceWithAuthority, ClassLevelAuthorityInterface {

        @Override
        public String interfaceMethodWithAuthority() {
            return "success";
        }

        @Override
        @HasAuthority(Authority.MEMBERS_MANAGE)
        public String methodOverridingInterfaceAuthority() {
            return "success";
        }

        @Override
        public String classLevelInterfaceMethod() {
            return "success";
        }
    }

    @Configuration
    @EnableMethodSecurity
    static class HasAuthorityInterceptorTestConfiguration {

        @Bean
        HasAuthorityMethodInterceptor testHasAuthorityMethodInterceptor() {
            return new HasAuthorityMethodInterceptor();
        }

        @Bean
        static DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
            DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
            creator.setProxyTargetClass(true);
            return creator;
        }

        @Bean
        TestService testService() {
            return new TestService();
        }

        @Bean
        ClassLevelAuthorizedService classLevelAuthorizedService() {
            return new ClassLevelAuthorizedService();
        }

        @Bean
        InterfaceImplementingService interfaceImplementingService() {
            return new InterfaceImplementingService();
        }
    }
}
