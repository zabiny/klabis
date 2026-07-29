package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AccountRootLinkProcessor Unit Tests")
class AccountRootLinkProcessorTest {

    private final AccountRootLinkProcessor processor = new AccountRootLinkProcessor();

    // HalFormsSupport.INSTANCE is a static shared across the whole test JVM, set only by
    // @PostConstruct on a real Spring context. Without wiring a real OwnershipResolver here, a
    // leftover INSTANCE from a *different* test class's context (whose resolver bean isn't this
    // test's concern) makes ownership checks fall through to false regardless of what this test
    // asserts. Same pattern as AccountMemberDetailLinkProcessorTest in this package.
    private Object previousInstance;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUpOwnershipResolver() throws Exception {
        ObjectProvider<OwnershipResolver> ownershipResolverProvider = mock(ObjectProvider.class);
        OwnershipResolver resolver = (ownerIdValue, authentication) -> {
            UUID ownerId = (UUID) ownerIdValue;
            return authentication instanceof KlabisJwtAuthenticationToken token
                   && token.getMemberIdUuid().map(ownerId::equals).orElse(false);
        };
        when(ownershipResolverProvider.getIfAvailable()).thenReturn(resolver);

        HalFormsSupport halFormsSupport = new HalFormsSupport(ownershipResolverProvider);
        previousInstance = instanceField().get(null);
        instanceField().set(null, halFormsSupport);
    }

    @AfterEach
    void clearSecurityContext() throws Exception {
        instanceField().set(null, previousInstance);
        SecurityContextHolder.clearContext();
    }

    private static java.lang.reflect.Field instanceField() throws NoSuchFieldException {
        java.lang.reflect.Field field = HalFormsSupport.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return field;
    }

    @Test
    @DisplayName("should add account link to root model when authenticated member has member profile")
    void shouldAddAccountLinkForAuthenticatedMember() {
        UUID memberId = UUID.randomUUID();
        setupAuthenticatedMemberContext(memberId);

        EntityModel<RootModel> rootModel = EntityModel.of(new RootModel());
        EntityModel<RootModel> result = processor.process(rootModel);

        assertThat(result.getLink("account")).isPresent();
        assertThat(result.getLink("account").get().getHref()).contains("/api/members/" + memberId + "/account");
    }

    @Test
    @DisplayName("should not add account link when user has no member profile")
    void shouldNotAddAccountLinkWhenNoMemberProfile() {
        setupAuthenticatedUserWithoutMemberProfile();

        EntityModel<RootModel> rootModel = EntityModel.of(new RootModel());
        EntityModel<RootModel> result = processor.process(rootModel);

        assertThat(result.getLink("account")).isEmpty();
    }

    @Test
    @DisplayName("should not add account link when user is not authenticated")
    void shouldNotAddAccountLinkWhenNotAuthenticated() {
        setupUnauthenticatedContext();

        EntityModel<RootModel> rootModel = EntityModel.of(new RootModel());
        EntityModel<RootModel> result = processor.process(rootModel);

        assertThat(result.getLink("account")).isEmpty();
    }

    private void setupAuthenticatedMemberContext(UUID memberId) {
        UserId userId = new UserId(UUID.randomUUID());
        KlabisJwtAuthenticationToken token = mock(KlabisJwtAuthenticationToken.class);
        when(token.getMemberIdUuid()).thenReturn(java.util.Optional.of(memberId));
        when(token.hasMemberProfile()).thenReturn(true);
        when(token.isAuthenticated()).thenReturn(true);
        // Empty authorities: the account link must appear via ownership (@OwnerVisible/@OwnerId),
        // not FINANCE:MANAGE — proves the ownership branch, not just the authority branch.
        doReturn(Collections.emptyList()).when(token).getAuthorities();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
    }

    private void setupAuthenticatedUserWithoutMemberProfile() {
        KlabisJwtAuthenticationToken token = mock(KlabisJwtAuthenticationToken.class);
        when(token.getMemberIdUuid()).thenReturn(java.util.Optional.empty());
        when(token.hasMemberProfile()).thenReturn(false);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
    }

    private void setupUnauthenticatedContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
