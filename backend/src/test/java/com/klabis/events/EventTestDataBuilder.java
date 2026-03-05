package com.klabis.events;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.domain.Event;
import com.klabis.members.MemberId;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.WebsiteUrl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventTestDataBuilder {

    private String name = "Test Event";
    private LocalDate eventDate = LocalDate.of(2025, 7, 10);
    private String location = "Test Location";
    private String organizer = "Test Organizer";
    private WebsiteUrl websiteUrl = null;
    private MemberId coordinatorId = null;
    private EventId eventId = new EventId(UUID.randomUUID());
    private List<EventRegistration> registrations = new ArrayList<>();
    private AuditMetadata auditMetadata = null;

    private EventTestDataBuilder() {
    }

    public static EventTestDataBuilder anEvent() {
        return new EventTestDataBuilder();
    }

    public static EventTestDataBuilder anEventWithId(EventId eventId) {
        EventTestDataBuilder result = anEvent();
        result.eventId = eventId;
        return result;
    }

    public EventTestDataBuilder addRegistration(EventRegistration registration) {
        registrations.add(registration);
        return this;
    }

    public EventTestDataBuilder addRegistrations(List<EventRegistration> registrations) {
        this.registrations.addAll(registrations);
        return this;
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

    public EventTestDataBuilder withCoordinator(MemberId coordinatorId) {
        this.coordinatorId = coordinatorId;
        return this;
    }

    public Event build() {
        return Event.reconstruct(eventId,
                name,
                eventDate,
                location,
                organizer,
                websiteUrl,
                coordinatorId,
                EventStatus.DRAFT,
                registrations,
                auditMetadata);
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
