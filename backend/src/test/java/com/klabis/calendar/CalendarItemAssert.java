package com.klabis.calendar;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.calendar.domain.ManualCalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.events.EventId;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import java.time.LocalDate;

public class CalendarItemAssert extends AbstractAssert<CalendarItemAssert, CalendarItem> {

    private CalendarItemAssert(CalendarItem actual) {
        super(actual, CalendarItemAssert.class);
    }

    public static final InstanceOfAssertFactory<CalendarItem, CalendarItemAssert> CALENDAR_ITEM_ASSERT_FACTORY =
            new InstanceOfAssertFactory<>(CalendarItem.class, CalendarItemAssert::assertThat);

    public static CalendarItemAssert assertThat(CalendarItem actual) {
        return new CalendarItemAssert(actual);
    }

    public CalendarItemAssert hasId(CalendarItemId expected) {
        isNotNull();
        if (!actual.getId().equals(expected)) {
            failWithMessage("Expected calendar item id to be <%s> but was <%s>", expected, actual.getId());
        }
        return this;
    }

    public CalendarItemAssert hasIdNotNull() {
        isNotNull();
        if (actual.getId() == null) {
            failWithMessage("Expected calendar item id to be not null");
        }
        return this;
    }

    public CalendarItemAssert hasName(String expected) {
        isNotNull();
        if (!actual.getName().equals(expected)) {
            failWithMessage("Expected calendar item name to be <%s> but was <%s>", expected, actual.getName());
        }
        return this;
    }

    public CalendarItemAssert hasDescription(String expected) {
        isNotNull();
        String actualDescription = actual.getDescription();
        if (expected == null) {
            if (actualDescription != null) {
                failWithMessage("Expected calendar item description to be null but was <%s>", actualDescription);
            }
        } else {
            if (!expected.equals(actualDescription)) {
                failWithMessage("Expected calendar item description to be <%s> but was <%s>", expected, actualDescription);
            }
        }
        return this;
    }

    public CalendarItemAssert hasStartDate(LocalDate expected) {
        isNotNull();
        if (!actual.getStartDate().equals(expected)) {
            failWithMessage("Expected calendar item start date to be <%s> but was <%s>", expected, actual.getStartDate());
        }
        return this;
    }

    public CalendarItemAssert hasEndDate(LocalDate expected) {
        isNotNull();
        if (!actual.getEndDate().equals(expected)) {
            failWithMessage("Expected calendar item end date to be <%s> but was <%s>", expected, actual.getEndDate());
        }
        return this;
    }

    public CalendarItemAssert hasEventId(EventId expected) {
        isNotNull();
        EventId actualEventId = actual instanceof EventCalendarItem linked ? linked.getEventId() : null;
        if (expected == null) {
            if (actualEventId != null) {
                failWithMessage("Expected event id to be null but was <%s>", actualEventId);
            }
        } else {
            if (actualEventId == null) {
                failWithMessage("Expected event id to be <%s> but was null", expected);
            } else if (!actualEventId.equals(expected)) {
                failWithMessage("Expected event id to be <%s> but was <%s>", expected, actualEventId);
            }
        }
        return this;
    }

    public CalendarItemAssert isEventLinked() {
        isNotNull();
        if (!(actual instanceof EventCalendarItem)) {
            failWithMessage("Expected calendar item to be event-linked but was <%s>", actual.getClass().getSimpleName());
        }
        return this;
    }

    public CalendarItemAssert isManual() {
        isNotNull();
        if (!(actual instanceof ManualCalendarItem)) {
            failWithMessage("Expected calendar item to be manual (ManualCalendarItem) but was <%s>", actual.getClass().getSimpleName());
        }
        return this;
    }
}
