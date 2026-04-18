package com.klabis.members.infrastructure.restapi;

import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.ui.EntityModelWithDomain;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.infrastructure.restapi.PermissionController;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.Link;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Unit tests for {@link MemberPermissionsLinkProcessor}.
 * <p>
 * Tests conditional HATEOAS link generation based on user authorities.
 */
@DisplayName("MemberPermissionsLinkProcessor Unit Tests")
class MemberPermissionsLinkProcessorTest {

    private MemberPermissionsLinkProcessor testedSubject;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        testedSubject = new MemberPermissionsLinkProcessor();
        SecurityContextHolder.clearContext();

        ObjectProvider<OwnershipResolver> ownershipResolverProvider = org.mockito.Mockito.mock(ObjectProvider.class);
        HalFormsSupport halFormsSupport = new HalFormsSupport(ownershipResolverProvider);
        java.lang.reflect.Field instanceField = HalFormsSupport.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, halFormsSupport);
    }

    @Test
    @DisplayName("should add permissions link for active member when user has MEMBERS:PERMISSIONS authority")
    void shouldAddPermissionsLinkWhenUserHasMembersPermissionsAuthority() {
        UUID memberUuid = UUID.randomUUID();
        EntityModelWithDomain<MemberDetailsResponse, Member> model = modelFor(memberUuid, true);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:PERMISSIONS")));

        testedSubject.process(model);

        Optional<Link> permissionsLink = model.getLink("permissions");
        assertThat(permissionsLink).isPresent();

        String expectedHref = linkTo(methodOn(PermissionController.class)
                .getUserPermissions(memberUuid))
                .toUri()
                .toString();
        assertThat(permissionsLink.get().getHref()).isEqualTo(expectedHref);
    }

    @Test
    @DisplayName("should not add permissions link when user lacks MEMBERS:PERMISSIONS authority")
    void shouldNotAddPermissionsLinkWhenUserLacksMembersPermissionsAuthority() {
        EntityModelWithDomain<MemberDetailsResponse, Member> model = modelFor(UUID.randomUUID(), true);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:READ")));

        testedSubject.process(model);

        assertThat(model.getLink("permissions")).isEmpty();
    }

    @Test
    @DisplayName("should not add permissions link when user is unauthenticated")
    void shouldNotAddPermissionsLinkWhenUserIsUnauthenticated() {
        EntityModelWithDomain<MemberDetailsResponse, Member> model = modelFor(UUID.randomUUID(), true);

        SecurityContextHolder.clearContext();

        testedSubject.process(model);

        assertThat(model.getLink("permissions")).isEmpty();
    }

    @Test
    @DisplayName("should not add permissions link when member is not active")
    void shouldNotAddPermissionsLinkWhenMemberIsNotActive() {
        EntityModelWithDomain<MemberDetailsResponse, Member> model = modelFor(UUID.randomUUID(), false);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:PERMISSIONS")));

        testedSubject.process(model);

        assertThat(model.getLink("permissions")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private EntityModelWithDomain<MemberDetailsResponse, Member> modelFor(UUID memberUuid, boolean active) {
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

        Member member = MemberTestDataBuilder.aMemberWithId(memberUuid)
                .withActive(active)
                .build();

        return (EntityModelWithDomain<MemberDetailsResponse, Member>)
                HalFormsSupport.entityModelWithDomain(response, member);
    }

    /**
     * Helper method to mock SecurityContext with specific authorities.
     */
    private void mockSecurityContext(Collection<? extends GrantedAuthority> authorities) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.isAuthenticated()).thenReturn(true);
        when(authenticationMock.getAuthorities()).thenAnswer(invocation -> authorities);

        SecurityContext securityContextMock = mock(SecurityContext.class);
        when(securityContextMock.getAuthentication()).thenReturn(authenticationMock);

        SecurityContextHolder.setContext(securityContextMock);
    }
}
