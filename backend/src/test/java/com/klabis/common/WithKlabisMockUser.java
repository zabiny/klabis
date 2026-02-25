package com.klabis.common;

import com.klabis.common.users.Authority;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(
        factory = WithKlabisMockUserSecurityContextFactory.class
)
public @interface WithKlabisMockUser {

    String userId() default "";

    String memberId() default "";

    String username() default "";

    Authority[] authorities() default {};
}
