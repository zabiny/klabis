package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.infrastructure.restapi.PermissionController;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
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

    @BeforeEach
    void setUp() {
        testedSubject = new MemberPermissionsLinkProcessor();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should add permissions link when user has MEMBERS:PERMISSIONS authority")
    void shouldAddPermissionsLinkWhenUserHasMembersPermissionsAuthority() {
        // Given: User with MEMBERS:PERMISSIONS authority
        UUID memberId = UUID.randomUUID();
        MemberDetailsResponse response = createMemberDetailsResponse(memberId);
        EntityModel<MemberDetailsResponse> model = EntityModel.of(response);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:PERMISSIONS")));

        // When: Process is called
        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        // Then: Permissions link is present
        Optional<Link> permissionsLink = result.getLink("permissions");
        assertThat(permissionsLink).isPresent();

        String expectedHref = linkTo(methodOn(PermissionController.class)
                .getUserPermissions(memberId))
                .toUri()
                .toString();
        assertThat(permissionsLink.get().getHref()).isEqualTo(expectedHref);
    }

    @Test
    @DisplayName("should not add permissions link when user lacks MEMBERS:PERMISSIONS authority")
    void shouldNotAddPermissionsLinkWhenUserLacksMembersPermissionsAuthority() {
        // Given: User without MEMBERS:PERMISSIONS authority
        UUID memberId = UUID.randomUUID();
        MemberDetailsResponse response = createMemberDetailsResponse(memberId);
        EntityModel<MemberDetailsResponse> model = EntityModel.of(response);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:READ")));

        // When: Process is called
        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        // Then: Permissions link is NOT present
        Optional<Link> permissionsLink = result.getLink("permissions");
        assertThat(permissionsLink).isEmpty();
    }

    @Test
    @DisplayName("should not add permissions link when user is unauthenticated")
    void shouldNotAddPermissionsLinkWhenUserIsUnauthenticated() {
        // Given: No authentication
        UUID memberId = UUID.randomUUID();
        MemberDetailsResponse response = createMemberDetailsResponse(memberId);
        EntityModel<MemberDetailsResponse> model = EntityModel.of(response);

        SecurityContextHolder.clearContext();

        // When: Process is called
        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        // Then: Permissions link is NOT present
        Optional<Link> permissionsLink = result.getLink("permissions");
        assertThat(permissionsLink).isEmpty();
    }

    @Test
    @DisplayName("should handle null id gracefully")
    void shouldHandleNullIdGracefully() {
        // Given: MemberDetailsResponse with null id
        MemberDetailsResponse response = createMemberDetailsResponse(null);
        EntityModel<MemberDetailsResponse> model = EntityModel.of(response);

        mockSecurityContext(List.of(new SimpleGrantedAuthority("MEMBERS:PERMISSIONS")));

        // When: Process is called
        EntityModel<MemberDetailsResponse> result = testedSubject.process(model);

        // Then: No exception thrown, no link added
        Optional<Link> permissionsLink = result.getLink("permissions");
        assertThat(permissionsLink).isEmpty();
    }

    /**
     * Helper method to create a minimal MemberDetailsResponse for testing.
     */
    private MemberDetailsResponse createMemberDetailsResponse(UUID id) {
        return new MemberDetailsResponse(
                new MemberId(id),
                "ZBM0101",
                "Jan",
                "Novák",
                LocalDate.of(1990, 1, 1),
                "CZ",
                null,
                "test@example.com",
                "+420777123456",
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
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
