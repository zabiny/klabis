package com.klabis.events;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithKlabisMockUser(
        username = "admin",
        memberId = AsEventCoordinator.MEMBER_ID,
        authorities = {Authority.EVENTS_READ},
        targetGrants = {
                @TargetGrant(authority = Authority.EVENTS_EDIT, type = TargetType.EVENT, ids = AsEventCoordinator.EVENT_ID),
                @TargetGrant(authority = Authority.EVENTS_REGISTRATIONS_READ, type = TargetType.EVENT, ids = AsEventCoordinator.EVENT_ID)
        })
public @interface AsEventCoordinator {

    String MEMBER_ID = "00000000-0000-0000-0000-000000000042";
    String EVENT_ID = "00000000-0000-0000-0000-0000000000e1";
}
