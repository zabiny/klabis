package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CalendarRootPostprocessor Unit Tests")
class CalendarRootPostprocessorTest {

    @Test
    @DisplayName("should add calendar link to root model")
    void shouldAddCalendarLinkToRootModel() {
        CalendarRootPostprocessor testedSubject = new CalendarRootPostprocessor();
        EntityModel<RootModel> rootModel = EntityModel.of(new RootModel());

        EntityModel<RootModel> result = testedSubject.process(rootModel);

        assertThat(result.getLink("calendar")).isPresent();
        assertThat(result.getLink("calendar").get().getHref()).contains("/api/calendar-items");
    }
}
