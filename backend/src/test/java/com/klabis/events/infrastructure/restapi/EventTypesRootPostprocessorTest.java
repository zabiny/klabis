package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import org.junit.jupiter.api.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventTypesRootPostprocessor Unit Tests (branches not reachable from a controller response; the rest is covered in EventTypeControllerTest)")
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
        @DisplayName("should NOT add event-types link when not authenticated")
        void shouldNotAddLinkWhenNotAuthenticated() {
            SecurityContextHolder.clearContext();

            EntityModel<RootModel> result = new EventTypesRootPostprocessor().process(EntityModel.of(new RootModel()));

            assertThat(result.getLink("event-types")).isEmpty();
        }
    }
}
