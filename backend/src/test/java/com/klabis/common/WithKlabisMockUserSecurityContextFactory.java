package com.klabis.common;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
final class WithKlabisMockUserSecurityContextFactory implements WithSecurityContextFactory<WithKlabisMockUser> {
    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public SecurityContext createSecurityContext(WithKlabisMockUser withUser) {
        final String userName = StringUtils.defaultIfBlank(withUser.username(), "ZBM8001");

        // if no userId is given, either use memberId if provided (as userId == memberId) or generate random (as userId is required)
        final UUID userId = UUID.fromString(StringUtils.defaultIfBlank(withUser.userId(),
                StringUtils.defaultIfBlank(withUser.memberId(), UUID.randomUUID().toString())));

        // if no memberId is given, do not generate random or use userId => memberId is not required (may be null), we can simulate users who are not members by that
        final UUID memberId = StringUtils.isBlank(withUser.memberId()) ? null : UUID.fromString(withUser.memberId());

        KlabisJwtAuthenticationToken authentication = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams(userName, userId).withMemberId(memberId).withAuthorities(withUser.authorities()),
                fixedSnapshot(withUser));
        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    private static AuthorizationSnapshot fixedSnapshot(WithKlabisMockUser withUser) {
        Map<Authority, Set<TargetRef>> overTargets = new EnumMap<>(Authority.class);
        for (TargetGrant grant : withUser.targetGrants()) {
            Set<TargetRef> targets = overTargets.computeIfAbsent(grant.authority(), a -> new HashSet<>());
            for (String id : grant.ids()) {
                targets.add(new TargetRef(grant.type(), UUID.fromString(id)));
            }
        }
        return AuthorizationSnapshot.of(Set.of(withUser.authorities()), overTargets);
    }

    @Autowired(required = false)
    void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }
}
