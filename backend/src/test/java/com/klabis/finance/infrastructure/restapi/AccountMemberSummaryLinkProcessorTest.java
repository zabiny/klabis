package com.klabis.finance.infrastructure.restapi;

import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AccountMemberSummaryLinkProcessor Unit Tests")
class AccountMemberSummaryLinkProcessorTest {

    private AccountMemberSummaryLinkProcessor testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new AccountMemberSummaryLinkProcessor();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should add account link to member summary when user has FINANCE:MANAGE authority")
    void shouldAddAccountLinkWhenUserHasFinanceManageAuthority() {
        UUID memberUuid = UUID.randomUUID();
        EntityModel<MemberSummaryResponse> model = modelFor(memberUuid, true);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("FINANCE:MANAGE")));

        EntityModel<MemberSummaryResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isPresent();
        assertThat(result.getLink("account").get().getHref())
                .contains("/api/members/" + memberUuid + "/account");
    }

    @Test
    @DisplayName("should not add account link when user lacks FINANCE:MANAGE authority")
    void shouldNotAddAccountLinkWhenUserLacksFinanceManageAuthority() {
        EntityModel<MemberSummaryResponse> model = modelFor(UUID.randomUUID(), true);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:READ")));

        EntityModel<MemberSummaryResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isEmpty();
    }

    @Test
    @DisplayName("should not add account link when user is unauthenticated")
    void shouldNotAddAccountLinkWhenUserIsUnauthenticated() {
        EntityModel<MemberSummaryResponse> model = modelFor(UUID.randomUUID(), true);

        SecurityContextHolder.clearContext();

        EntityModel<MemberSummaryResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isEmpty();
    }

    @Test
    @DisplayName("should add account link for inactive member when user has FINANCE:MANAGE")
    void shouldAddAccountLinkForInactiveMemberWhenUserHasFinanceManage() {
        UUID memberUuid = UUID.randomUUID();
        EntityModel<MemberSummaryResponse> model = modelFor(memberUuid, false);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("FINANCE:MANAGE")));

        EntityModel<MemberSummaryResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isPresent();
        assertThat(result.getLink("account").get().getHref())
                .contains("/api/members/" + memberUuid + "/account");
    }

    private EntityModel<MemberSummaryResponse> modelFor(UUID memberUuid, boolean active) {
        MemberSummaryResponse response = new MemberSummaryResponse(
                new MemberId(memberUuid),
                "Jan",
                "Novák",
                "ZBM0101",
                "test@example.com",
                active
        );
        return EntityModel.of(response);
    }

    private void mockSecurityContext(Collection<? extends GrantedAuthority> authorities) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.isAuthenticated()).thenReturn(true);
        when(authenticationMock.getAuthorities()).thenAnswer(invocation -> authorities);

        SecurityContext securityContextMock = mock(SecurityContext.class);
        when(securityContextMock.getAuthentication()).thenReturn(authenticationMock);

        SecurityContextHolder.setContext(securityContextMock);
    }
}
