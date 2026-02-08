package com.klabis.events;

import com.klabis.users.UserId;

import java.time.LocalDate;
import java.util.UUID;

public class EventTestDataBuilder {

    private String name = "Test Event";
    private LocalDate eventDate = LocalDate.of(2025, 7, 10);
    private String location = "Test Location";
    private String organizer = "Test Organizer";
    private WebsiteUrl websiteUrl = null;
    private UserId coordinatorId = null;

    private EventTestDataBuilder() {
    }

    public static EventTestDataBuilder anEvent() {
        return new EventTestDataBuilder();
    }

    public EventTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EventTestDataBuilder withDate(LocalDate eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public EventTestDataBuilder withLocation(String location) {
        this.location = location;
        return this;
    }

    public EventTestDataBuilder withOrganizer(String organizer) {
        this.organizer = organizer;
        return this;
    }

    public EventTestDataBuilder withWebsiteUrl(String url) {
        this.websiteUrl = WebsiteUrl.of(url);
        return this;
    }

    public EventTestDataBuilder withCoordinator(UserId coordinatorId) {
        this.coordinatorId = coordinatorId;
        return this;
    }

    public Event build() {
        return Event.create(name, eventDate, location, organizer, websiteUrl, coordinatorId);
    }

    public Event buildPublished() {
        Event event = build();
        event.publish();
        return event;
    }

    public Event buildFinished() {
        Event event = buildPublished();
        event.finish();
        return event;
    }

    public Event buildCancelled() {
        Event event = build();
        event.cancel();
        return event;
    }
}
