package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.DashboardModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;


import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DashboardUpcomingRegistrationsLinkProcessor (branches not reachable from a controller response; the rest is covered in EventControllerTest)")
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
    @DisplayName("does not add upcomingRegistrations link when user is unauthenticated")
    void shouldNotAddUpcomingRegistrationsLinkWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        EntityModel<DashboardModel> model = EntityModel.of(new DashboardModel());
        EntityModel<DashboardModel> result = processor.process(model);

        assertThat(result.getLink("upcomingRegistrations")).isEmpty();
    }
}
