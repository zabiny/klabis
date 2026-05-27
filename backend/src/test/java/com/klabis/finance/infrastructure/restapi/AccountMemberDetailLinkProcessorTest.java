package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AccountMemberDetailLinkProcessor Unit Tests")
class AccountMemberDetailLinkProcessorTest {

    private AccountMemberDetailLinkProcessor testedSubject;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        testedSubject = new AccountMemberDetailLinkProcessor();
        SecurityContextHolder.clearContext();

        ObjectProvider<OwnershipResolver> ownershipResolverProvider = mock(ObjectProvider.class);
        HalFormsSupport halFormsSupport = new HalFormsSupport(ownershipResolverProvider);
        java.lang.reflect.Field instanceField = HalFormsSupport.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, halFormsSupport);
    }

    @Test
    @DisplayName("should add account link to member detail when user has FINANCE:MANAGE authority")
    void shouldAddAccountLinkWhenUserHasFinanceManageAuthority() {
        UUID memberUuid = UUID.randomUUID();
        EntityModel<MemberDetailsResponse> model = modelFor(memberUuid, true);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("FINANCE:MANAGE")));

        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isPresent();
        assertThat(result.getLink("account").get().getHref())
                .contains("/api/members/" + memberUuid + "/account");
    }

    @Test
    @DisplayName("should not add account link when user lacks FINANCE:MANAGE authority")
    void shouldNotAddAccountLinkWhenUserLacksFinanceManageAuthority() {
        EntityModel<MemberDetailsResponse> model = modelFor(UUID.randomUUID(), true);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:READ")));

        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isEmpty();
    }

    @Test
    @DisplayName("should not add account link when user is unauthenticated")
    void shouldNotAddAccountLinkWhenUserIsUnauthenticated() {
        EntityModel<MemberDetailsResponse> model = modelFor(UUID.randomUUID(), true);

        SecurityContextHolder.clearContext();

        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isEmpty();
    }

    @Test
    @DisplayName("should add account link even for inactive members when user has FINANCE:MANAGE")
    void shouldAddAccountLinkForInactiveMemberWhenUserHasFinanceManage() {
        UUID memberUuid = UUID.randomUUID();
        EntityModel<MemberDetailsResponse> model = modelFor(memberUuid, false);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("FINANCE:MANAGE")));

        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        assertThat(result.getLink("account")).isPresent();
        assertThat(result.getLink("account").get().getHref())
                .contains("/api/members/" + memberUuid + "/account");
    }

    private EntityModel<MemberDetailsResponse> modelFor(UUID memberUuid, boolean active) {
        MemberDetailsResponse response = MemberDetailsResponseBuilder.builder()
                .id(new MemberId(memberUuid))
                .registrationNumber("ZBM0101")
                .firstName("Jan")
                .lastName("Novák")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .nationality("CZ")
                .email("test@example.com")
                .phone("+420777123456")
                .active(active)
                .build();

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
