package com.klabis.common;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
final class WithKlabisMockUserSecurityContextFactory implements WithSecurityContextFactory<WithKlabisMockUser> {
    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public SecurityContext createSecurityContext(WithKlabisMockUser withUser) {
        final String userName = StringUtils.defaultIfBlank(withUser.username(), "ZBM8001");

        final UUID userId = UUID.fromString(StringUtils.defaultIfBlank(withUser.userId(),
                StringUtils.defaultIfBlank(withUser.memberId(), UUID.randomUUID().toString())));

        final UUID memberId = StringUtils.isBlank(withUser.memberId()) ? userId : UUID.fromString(withUser.memberId());

        KlabisJwtAuthenticationToken authentication = KlabisAuthenticationFactory.createAuthenticationToken(JwtParams.jwtTokenParams(
                userName,
                userId).withMemberId(memberId).withAuthorities(withUser.authorities()));
        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    @Autowired(required = false)
    void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }
}
