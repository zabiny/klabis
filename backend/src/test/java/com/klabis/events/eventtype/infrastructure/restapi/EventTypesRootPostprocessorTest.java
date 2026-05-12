package com.klabis.events.eventtype.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventTypesRootPostprocessor Unit Tests")
class EventTypesRootPostprocessorTest {

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
    @DisplayName("event-types nav link visibility")
    class NavLinkVisibility {

        @Test
        @DisplayName("should add event-types link for admin user with EVENTS:READ and EVENTS:MANAGE")
        void shouldAddLinkForEventsManageUser() {
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("admin", "pw",
                            List.of(new SimpleGrantedAuthority(Authority.EVENTS_READ.getValue()),
                                    new SimpleGrantedAuthority(Authority.EVENTS_MANAGE.getValue()))));

            EntityModel<RootModel> result = new EventTypesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("event-types")).isPresent();
            assertThat(result.getLink("event-types").get().getHref()).contains("/api/event-types");
        }

        @Test
        @DisplayName("should NOT add event-types link for user with only EVENTS:READ authority")
        void shouldNotAddLinkForEventsReadOnlyUser() {
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("member", "pw",
                            List.of(new SimpleGrantedAuthority(Authority.EVENTS_READ.getValue()))));

            EntityModel<RootModel> result = new EventTypesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("event-types")).isEmpty();
        }

        @Test
        @DisplayName("should NOT add event-types link when not authenticated")
        void shouldNotAddLinkWhenNotAuthenticated() {
            SecurityContextHolder.clearContext();

            EntityModel<RootModel> result = new EventTypesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("event-types")).isEmpty();
        }
    }
}
