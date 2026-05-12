package com.klabis.events.eventtype.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventTypesRootPostprocessor Unit Tests")
class EventTypesRootPostprocessorTest {

    @BeforeEach
    void setUpRequestContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("should add event-types link to root model")
    void shouldAddEventTypesLinkToRootModel() {
        EventTypesRootPostprocessor testedSubject = new EventTypesRootPostprocessor();
        EntityModel<RootModel> rootModel = EntityModel.of(new RootModel());

        EntityModel<RootModel> result = testedSubject.process(rootModel);

        assertThat(result.getLink("event-types")).isPresent();
        assertThat(result.getLink("event-types").get().getHref()).contains("/api/event-types");
    }
}
