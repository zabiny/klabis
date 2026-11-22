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

    // defines userId. If missing, random value is used as user Id.
    String userId() default "";

    // Defines memberId for user (if not defined, it's considered that user doesn't have Member instance). If memberId is defined and userId is missing, then same value is used as userId
    String memberId() default "";

    // affects Authentication.getName.
    String username() default "";

    // authorities what will be available for authenticated user. Allows to easily test authorizations of the API endpoints.
    Authority[] authorities() default {};

    // what SelfRelationshipSource grants the user over themself in production (slice tests have no relationship sources). MEMBER and MINOR_MEMBER need memberId, NON_MEMBER (a user without member profile, e.g. a legal guardian) needs userId
    SelfGrants selfGrants() default SelfGrants.NONE;

    // authorities held only over specific targets, on top of the (global) authorities above
    TargetGrant[] targetGrants() default {};
}
