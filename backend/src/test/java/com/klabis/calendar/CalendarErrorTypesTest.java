package com.klabis.calendar;

import com.klabis.calendar.application.CalendarNotFoundException;
import com.klabis.calendar.application.DateRangeTooLongException;
import com.klabis.calendar.domain.CalendarItemEndBeforeStartException;
import com.klabis.calendar.domain.CalendarItemReadOnlyException;
import com.klabis.common.exceptions.UserFacingError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Calendar error types")
class CalendarErrorTypesTest {

    private static void assertError(UserFacingError error, String type, String field, Map<String, Object> parameters) {
        assertThat(error.errorType()).isEqualTo(type);
        assertThat(error.field()).isEqualTo(field);
        assertThat(error.messageParameters()).containsExactlyInAnyOrderEntriesOf(parameters);
    }

    @Test
    @DisplayName("calendar item errors")
    void calendarItemErrors() {
        UUID id = UUID.randomUUID();
        CalendarNotFoundException notFound = new CalendarNotFoundException(id);

        assertError(notFound, "calendar-item-not-found", null, Map.of());
        assertThat(notFound.messageParameters().values().toString()).doesNotContain(id.toString());
        assertError(new CalendarItemReadOnlyException(), "calendar-item-read-only", null, Map.of());
        assertError(new CalendarItemEndBeforeStartException(), "calendar-item-end-before-start", "endDate", Map.of());
    }

    @Test
    @DisplayName("a date range longer than allowed names the limit")
    void dateRangeTooLong() {
        assertError(new DateRangeTooLongException(366, 368), "date-range-too-long", null, Map.of("maxDays", 366));
    }

    @Test
    @DisplayName("bound field exists as a property of the calendar item requests")
    void boundFieldIsRequestProperty() throws Exception {
        for (String request : new String[]{"CreateCalendarItemRequest", "UpdateCalendarItemRequest"}) {
            Set<String> components = Set.copyOf(Arrays.stream(
                            Class.forName("com.klabis.calendar.infrastructure.restapi." + request).getRecordComponents())
                    .map(RecordComponent::getName).toList());
            assertThat(components).contains("endDate");
        }
    }
}
