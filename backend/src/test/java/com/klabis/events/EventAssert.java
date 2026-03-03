package com.klabis.events;

import com.klabis.common.users.UserId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.WebsiteUrl;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;

import java.time.LocalDate;

public class EventAssert extends AbstractAssert<EventAssert, Event> {

    private EventAssert(Event actual) {
        super(actual, EventAssert.class);
    }

    public static final InstanceOfAssertFactory<Event, EventAssert> EVENT_ASSERT_FACTORY = new InstanceOfAssertFactory<>(Event.class,
            EventAssert::assertThat);

    public static EventAssert assertThat(Event actual) {
        return new EventAssert(actual);
    }

    public EventAssert hasStatus(EventStatus expected) {
        isNotNull();
        if (actual.getStatus() != expected) {
            failWithMessage("Expected event status to be <%s> but was <%s>", expected, actual.getStatus());
        }
        return this;
    }

    public EventAssert hasName(String expected) {
        isNotNull();
        if (!actual.getName().equals(expected)) {
            failWithMessage("Expected event name to be <%s> but was <%s>", expected, actual.getName());
        }
        return this;
    }

    public EventAssert hasDate(LocalDate expected) {
        isNotNull();
        if (!actual.getEventDate().equals(expected)) {
            failWithMessage("Expected event date to be <%s> but was <%s>", expected, actual.getEventDate());
        }
        return this;
    }

    public EventAssert hasLocation(String expected) {
        isNotNull();
        if (!actual.getLocation().equals(expected)) {
            failWithMessage("Expected event location to be <%s> but was <%s>", expected, actual.getLocation());
        }
        return this;
    }

    public EventAssert hasOrganizer(String expected) {
        isNotNull();
        if (!actual.getOrganizer().equals(expected)) {
            failWithMessage("Expected event organizer to be <%s> but was <%s>", expected, actual.getOrganizer());
        }
        return this;
    }

    public EventAssert hasId(EventId expected) {
        isNotNull();
        if (!actual.getId().equals(expected)) {
            failWithMessage("Expected event id to be <%s> but was <%s>", expected, actual.getId());
        }
        return this;
    }

    public EventAssert hasWebsiteUrl(WebsiteUrl expected) {
        isNotNull();
        if (expected == null) {
            if (actual.getWebsiteUrl() != null) {
                failWithMessage("Expected website url to be null but was <%s>", actual.getWebsiteUrl());
            }
        } else {
            if (actual.getWebsiteUrl() == null) {
                failWithMessage("Expected website url to be <%s> but was null", expected);
            } else if (!actual.getWebsiteUrl().equals(expected)) {
                failWithMessage("Expected website url to be <%s> but was <%s>", expected, actual.getWebsiteUrl());
            }
        }
        return this;
    }

    public EventAssert hasEventCoordinatorId(UserId expected) {
        isNotNull();
        if (expected == null) {
            if (actual.getEventCoordinatorId() != null) {
                failWithMessage("Expected event coordinator id to be null but was <%s>",
                        actual.getEventCoordinatorId());
            }
        } else {
            if (actual.getEventCoordinatorId() == null) {
                failWithMessage("Expected event coordinator id to be <%s> but was null", expected);
            } else if (!actual.getEventCoordinatorId().equals(expected)) {
                failWithMessage("Expected event coordinator id to be <%s> but was <%s>",
                        expected,
                        actual.getEventCoordinatorId());
            }
        }
        return this;
    }

    public EventAssert hasIdNotNull() {
        isNotNull();
        if (actual.getId() == null) {
            failWithMessage("Expected event id to be not null");
        }
        return this;
    }

    public EventAssert hasEmptyRegistrations() {
        isNotNull();
        if (actual.getRegistrations() != null  && !actual.getRegistrations().isEmpty()) {
            failWithMessage("Expected event registrations to be empty but contained %d registrations".formatted(actual.getRegistrations().size()));
        }
        return this;
    }

    public ListAssert<EventRegistration> getRegistrations() {
        isNotNull();
        return Assertions.assertThat(actual.getRegistrations());
    }
}
