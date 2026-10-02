package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.RootModel;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import com.klabis.members.infrastructure.restapi.MemberSummaryResponse;
import com.klabis.members.infrastructure.restapi.MemberSummaryResponseBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Defensive branches of the account link processors that cannot be reached through a controller response
 * (the member responses always carry an id, the member profile always has a member id).
 */
@DisplayName("Account link processors - branches unreachable from a response")
class AccountLinkProcessorBranchesTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("detail processor adds no link when the response has no id")
    void detailProcessorAddsNoLinkWithoutId() {
        authenticateAsFinanceManager();
        EntityModel<MemberDetailsResponse> model = EntityModel.of(MemberDetailsResponseBuilder.builder().build());

        var result = new AccountMemberDetailLinkProcessor().process(model);

        assertThat(result.getLinks()).isEmpty();
    }

    @Test
    @DisplayName("detail processor adds no link when the model has no content")
    void detailProcessorAddsNoLinkWithoutContent() {
        authenticateAsFinanceManager();
        EntityModel<MemberDetailsResponse> model = mock(EntityModel.class);
        when(model.getContent()).thenReturn(null);

        new AccountMemberDetailLinkProcessor().process(model);

        org.mockito.Mockito.verify(model, org.mockito.Mockito.never()).add(org.mockito.ArgumentMatchers.any(org.springframework.hateoas.Link.class));
    }

    @Test
    @DisplayName("summary processor adds no link when the response has no id")
    void summaryProcessorAddsNoLinkWithoutId() {
        authenticateAsFinanceManager();
        EntityModel<MemberSummaryResponse> model = EntityModel.of(MemberSummaryResponseBuilder.builder().build());

        var result = new AccountMemberSummaryLinkProcessor().process(model);

        assertThat(result.getLinks()).isEmpty();
    }

    @Test
    @DisplayName("summary processor adds no link when the model has no content")
    void summaryProcessorAddsNoLinkWithoutContent() {
        authenticateAsFinanceManager();
        EntityModel<MemberSummaryResponse> model = mock(EntityModel.class);
        when(model.getContent()).thenReturn(null);

        new AccountMemberSummaryLinkProcessor().process(model);

        org.mockito.Mockito.verify(model, org.mockito.Mockito.never()).add(org.mockito.ArgumentMatchers.any(org.springframework.hateoas.Link.class));
    }

    @Test
    @DisplayName("root processor adds no link when the member profile has no member id")
    void rootProcessorAddsNoLinkWithoutMemberId() {
        KlabisJwtAuthenticationToken token = mock(KlabisJwtAuthenticationToken.class);
        when(token.hasMemberProfile()).thenReturn(true);
        when(token.getMemberIdUuid()).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(token);

        var result = new AccountRootLinkProcessor().process(EntityModel.of(new RootModel()));

        assertThat(result.getLinks()).isEmpty();
    }

    private void authenticateAsFinanceManager() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "user", "n/a", List.of(new SimpleGrantedAuthority("FINANCE:MANAGE"))));
    }
}
