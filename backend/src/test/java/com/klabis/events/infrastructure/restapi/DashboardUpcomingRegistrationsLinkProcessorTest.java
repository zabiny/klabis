package com.klabis.events.infrastructure.restapi;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.DashboardModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DashboardUpcomingRegistrationsLinkProcessor")
class DashboardUpcomingRegistrationsLinkProcessorTest {

    private DashboardUpcomingRegistrationsLinkProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DashboardUpcomingRegistrationsLinkProcessor();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("adds upcomingRegistrations link when authenticated user has a member profile")
    void shouldAddUpcomingRegistrationsLinkWhenUserHasMemberProfile() {
        UUID memberIdUuid = UUID.randomUUID();
        authenticateWithMemberId(memberIdUuid);

        EntityModel<DashboardModel> model = EntityModel.of(new DashboardModel());
        EntityModel<DashboardModel> result = processor.process(model);

        Optional<Link> link = result.getLink("upcomingRegistrations");
        assertThat(link).isPresent();

        String href = link.get().getHref();
        assertThat(href).contains("registeredBy=me");
        assertThat(href).contains("dateFrom=" + LocalDate.now());
        assertThat(href).contains("sort=eventDate,ASC");
        assertThat(href).contains("size=3");
        assertThat(href).startsWith("/api/events");
    }

    @Test
    @DisplayName("does not add upcomingRegistrations link when authenticated user has no member profile")
    void shouldNotAddUpcomingRegistrationsLinkWhenUserHasNoMemberProfile() {
        authenticateWithoutMemberId();

        EntityModel<DashboardModel> model = EntityModel.of(new DashboardModel());
        EntityModel<DashboardModel> result = processor.process(model);

        assertThat(result.getLink("upcomingRegistrations")).isEmpty();
    }

    @Test
    @DisplayName("does not add upcomingRegistrations link when user is unauthenticated")
    void shouldNotAddUpcomingRegistrationsLinkWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        EntityModel<DashboardModel> model = EntityModel.of(new DashboardModel());
        EntityModel<DashboardModel> result = processor.process(model);

        assertThat(result.getLink("upcomingRegistrations")).isEmpty();
    }

    private void authenticateWithMemberId(UUID memberIdUuid) {
        KlabisJwtAuthenticationToken auth = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("testuser", UUID.randomUUID())
                        .withMemberId(memberIdUuid));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateWithoutMemberId() {
        KlabisJwtAuthenticationToken auth = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("adminuser", UUID.randomUUID())
                        .withMemberId((UUID) null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
