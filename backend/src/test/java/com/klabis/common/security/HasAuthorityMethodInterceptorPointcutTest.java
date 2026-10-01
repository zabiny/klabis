package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HasAuthorityMethodInterceptor pointcut")
class HasAuthorityMethodInterceptorPointcutTest {

    static class SecuredService {
        @HasAuthority(Authority.MEMBERS_READ)
        public String secured() {
            return "secured";
        }
    }

    private final HasAuthorityMethodInterceptor interceptor = new HasAuthorityMethodInterceptor();

    @Test
    @DisplayName("class filter accepts application classes")
    void classFilterAcceptsApplicationClasses() {
        ClassFilter classFilter = interceptor.getPointcut().getClassFilter();

        assertThat(classFilter.matches(SecuredService.class)).isTrue();
    }

    @Test
    @DisplayName("class filter rejects framework and JDK classes")
    void classFilterRejectsNonApplicationClasses() {
        ClassFilter classFilter = interceptor.getPointcut().getClassFilter();

        assertThat(classFilter.matches(String.class)).isFalse();
        assertThat(classFilter.matches(ClassFilter.class)).isFalse();
    }

    @Test
    @DisplayName("method matcher matches @HasAuthority method of an application class")
    void methodMatcherMatchesAnnotatedMethod() throws NoSuchMethodException {
        MethodMatcher methodMatcher = interceptor.getPointcut().getMethodMatcher();

        assertThat(methodMatcher.matches(SecuredService.class.getMethod("secured"), SecuredService.class)).isTrue();
        assertThat(methodMatcher.matches(SecuredService.class.getMethod("toString"), SecuredService.class)).isFalse();
    }
}
