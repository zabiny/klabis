package com.klabis.finance.infrastructure.restapi;

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
        memberId = AsAccountOwner.MEMBER_ID,
        targetGrants = @TargetGrant(authority = Authority.FINANCE_VIEW, type = TargetType.MEMBER, ids = AsAccountOwner.MEMBER_ID))
@interface AsAccountOwner {

    String MEMBER_ID = "11111111-1111-1111-1111-111111111111";
}
