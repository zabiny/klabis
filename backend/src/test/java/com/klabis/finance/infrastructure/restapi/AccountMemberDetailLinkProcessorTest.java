package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.SecurityContextAuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountMemberDetailLinkProcessor")
class AccountMemberDetailLinkProcessorTest {

    private static final UUID SELF = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        var token = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("user", new com.klabis.common.users.UserId(SELF)).withMemberId(new MemberId(SELF)),
                TestSnapshots.of(overAll, overTargets));
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private boolean hasAccountLinkOn(UUID memberId) {
        EntityModel<MemberDetailsResponse> model = EntityModel.of(MemberDetailsResponseBuilder.builder().id(memberId).build());
        return new AccountMemberDetailLinkProcessor(SecurityContextAuthorizationEvaluator.create())
                .process(model).getLink("account").isPresent();
    }

    @Test
    @DisplayName("is absent on the user's own profile")
    void absentOnOwnProfile() {
        authenticate(Set.of(), Map.of(Authority.FINANCE_VIEW, Set.of(TargetRef.member(SELF))));

        assertThat(hasAccountLinkOn(SELF)).isFalse();
    }

    @Test
    @DisplayName("is absent on an unrelated member's profile without FINANCE:MANAGE")
    void absentOnUnrelatedProfile() {
        authenticate(Set.of(), Map.of(Authority.FINANCE_VIEW, Set.of(TargetRef.member(SELF))));

        assertThat(hasAccountLinkOn(OTHER)).isFalse();
    }

    @Test
    @DisplayName("is present for a finance manager")
    void presentForFinanceManager() {
        authenticate(Set.of(Authority.FINANCE_MANAGE), Map.of());

        assertThat(hasAccountLinkOn(OTHER)).isTrue();
    }

    @Test
    @DisplayName("is present for a user holding FINANCE:VIEW over the displayed member")
    void presentForFinanceViewHolder() {
        authenticate(Set.of(), Map.of(Authority.FINANCE_VIEW, Set.of(TargetRef.member(OTHER))));

        assertThat(hasAccountLinkOn(OTHER)).isTrue();
    }
}
