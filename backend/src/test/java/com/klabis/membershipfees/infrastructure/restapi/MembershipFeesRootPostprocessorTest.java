package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MembershipFeesRootPostprocessor Unit Tests")
class MembershipFeesRootPostprocessorTest {

    @BeforeEach
    void setUpRequestContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("membership-fees nav link visibility")
    class NavLinkVisibility {

        @Test
        @DisplayName("should add membership-fees link for admin user with MEMBERS:MANAGE authority")
        void shouldAddLinkForMembersManageUser() {
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("admin", "pw",
                            List.of(new SimpleGrantedAuthority(Authority.MEMBERS_MANAGE.getValue()))));

            EntityModel<RootModel> result = new MembershipFeesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("membership-fees")).isPresent();
            assertThat(result.getLink("membership-fees").get().getHref()).contains("/api/membership-fee-tiers");
        }

        @Test
        @DisplayName("should NOT add membership-fees link for non-admin user without MEMBERS:MANAGE authority")
        void shouldNotAddLinkForNonAdminUser() {
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("member", "pw",
                            List.of(new SimpleGrantedAuthority(Authority.MEMBERS_READ.getValue()))));

            EntityModel<RootModel> result = new MembershipFeesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("membership-fees")).isEmpty();
        }

        @Test
        @DisplayName("should NOT add membership-fees link when not authenticated")
        void shouldNotAddLinkWhenNotAuthenticated() {
            SecurityContextHolder.clearContext();

            EntityModel<RootModel> result = new MembershipFeesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("membership-fees")).isEmpty();
        }
    }
}
