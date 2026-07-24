package com.klabis.events.infrastructure.restapi;

import com.klabis.finance.application.FinanceAccountLinkSupport;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationRecordTransactionLinkProcessor Unit Tests")
class RegistrationRecordTransactionLinkProcessorTest {

    @Mock
    private FinanceAccountLinkSupport financeAccountLinkSupport;

    private RegistrationRecordTransactionLinkProcessor testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new RegistrationRecordTransactionLinkProcessor(financeAccountLinkSupport);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("adds recordTransaction link pointing to member account when caller has FINANCE:MANAGE")
    void shouldAddRecordTransactionLinkWhenCallerHasFinanceManage() {
        UUID memberUuid = UUID.randomUUID();
        EntityModel<RegistrationSummaryDto> model = modelFor(memberUuid);
        when(financeAccountLinkSupport.accountLink(memberUuid))
                .thenReturn(Optional.of(Link.of("/api/members/" + memberUuid + "/account", "account")));

        mockSecurityContext(List.of(new SimpleGrantedAuthority("FINANCE:MANAGE")));

        EntityModel<RegistrationSummaryDto> result = testedSubject.process(model);

        assertThat(result.getLink("recordTransaction")).isPresent();
        assertThat(result.getLink("recordTransaction").get().getHref())
                .contains("/api/members/" + memberUuid + "/account");
    }

    @Test
    @DisplayName("does not add recordTransaction link when caller lacks FINANCE:MANAGE")
    void shouldNotAddLinkWhenCallerLacksFinanceManage() {
        EntityModel<RegistrationSummaryDto> model = modelFor(UUID.randomUUID());

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:READ")));

        EntityModel<RegistrationSummaryDto> result = testedSubject.process(model);

        assertThat(result.getLink("recordTransaction")).isEmpty();
    }

    @Test
    @DisplayName("does not add recordTransaction link when caller is unauthenticated")
    void shouldNotAddLinkWhenUnauthenticated() {
        EntityModel<RegistrationSummaryDto> model = modelFor(UUID.randomUUID());

        SecurityContextHolder.clearContext();

        EntityModel<RegistrationSummaryDto> result = testedSubject.process(model);

        assertThat(result.getLink("recordTransaction")).isEmpty();
    }

    @Test
    @DisplayName("does not add recordTransaction link when registeredMemberId is null")
    void shouldNotAddLinkWhenRegisteredMemberIdIsNull() {
        RegistrationSummaryDto dto = new RegistrationSummaryDto("John", "Doe", null, Instant.now(), Set.of(), null);
        EntityModel<RegistrationSummaryDto> model = EntityModel.of(dto);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("FINANCE:MANAGE")));

        EntityModel<RegistrationSummaryDto> result = testedSubject.process(model);

        assertThat(result.getLink("recordTransaction")).isEmpty();
    }

    private EntityModel<RegistrationSummaryDto> modelFor(UUID memberUuid) {
        RegistrationSummaryDto dto = new RegistrationSummaryDto(
                "Jan", "Novák", "M21", Instant.now(), Set.of(), new MemberId(memberUuid)
        );
        return EntityModel.of(dto);
    }

    private void mockSecurityContext(Collection<? extends GrantedAuthority> authorities) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getAuthorities()).thenAnswer(invocation -> authorities);

        SecurityContext securityContextMock = mock(SecurityContext.class);
        when(securityContextMock.getAuthentication()).thenReturn(authenticationMock);

        SecurityContextHolder.setContext(securityContextMock);
    }
}
