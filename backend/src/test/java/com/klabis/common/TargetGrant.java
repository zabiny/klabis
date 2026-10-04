package com.klabis.common;

import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authority held over specific targets, for {@link WithKlabisMockUser#targetGrants()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TargetGrant {

    Authority authority();

    TargetType type();

    // UUIDs of the targets the authority is held over
    String[] ids();
}
