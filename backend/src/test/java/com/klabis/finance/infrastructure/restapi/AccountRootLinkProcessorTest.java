package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.SecurityContextAuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberDtoBuilder;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AccountRootLinkProcessor")
class AccountRootLinkProcessorTest {

    private static final UUID PARENT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CHILD_NOVAK = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID CHILD_ADAMEC = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");

    private final Members members = mock(Members.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(JwtParams params, Set<UUID> viewable) {
        Set<TargetRef> targets = new java.util.HashSet<>();
        viewable.forEach(id -> targets.add(TargetRef.member(id)));
        SecurityContextHolder.getContext().setAuthentication(KlabisAuthenticationFactory.createAuthenticationToken(
                params, TestSnapshots.of(Set.of(), Map.of(Authority.FINANCE_VIEW, targets))));
    }

    private void stubMember(Map<UUID, String[]> names) {
        Map<MemberId, MemberDto> byId = new LinkedHashMap<>();
        names.forEach((id, n) -> byId.put(new MemberId(id),
                MemberDtoBuilder.builder().memberId(id).firstName(n[0]).lastName(n[1]).build()));
        when(members.findByIds(any())).thenReturn(byId);
    }

    private EntityModel<RootModel> process() {
        var processor = new AccountRootLinkProcessor(
                new ViewableAccounts(SecurityContextAuthorizationEvaluator.create(), members));
        return processor.process(EntityModel.of(new RootModel()));
    }

    @Test
    @DisplayName("member links to their own account")
    void memberLinksToOwnAccount() {
        authenticate(JwtParams.jwtTokenParams("member", new UserId(PARENT)).withMemberId(new MemberId(PARENT)),
                Set.of(PARENT, CHILD_NOVAK));
        stubMember(Map.of(PARENT, new String[]{"Petr", "Zeman"}, CHILD_NOVAK, new String[]{"Anna", "Novak"}));

        assertThat(process().getLink("account").orElseThrow().getHref()).contains("/api/members/" + PARENT + "/account");
    }

    @Test
    @DisplayName("non-member guardian links to the first minor by last and first name")
    void guardianLinksToFirstMinor() {
        authenticate(JwtParams.jwtTokenParams("EXT0001", new UserId(UUID.randomUUID())), Set.of(CHILD_NOVAK, CHILD_ADAMEC));
        stubMember(Map.of(CHILD_NOVAK, new String[]{"Anna", "Novak"}, CHILD_ADAMEC, new String[]{"Bob", "Adamec"}));

        assertThat(process().getLink("account").orElseThrow().getHref())
                .contains("/api/members/" + CHILD_ADAMEC + "/account");
    }

    @Test
    @DisplayName("user without any viewable account gets no link")
    void noViewableAccountMeansNoLink() {
        authenticate(JwtParams.jwtTokenParams("admin", new UserId(UUID.randomUUID())), Set.of());

        assertThat(process().getLink("account")).isEmpty();
    }
}
