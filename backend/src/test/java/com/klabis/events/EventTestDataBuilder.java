package com.klabis.events;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.events.EventTypeId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class EventTestDataBuilder {

    private String name = "Test Event";
    private LocalDate eventDate = LocalDate.of(2025, 7, 10);
    private String location = "Test Location";
    private String organizer = "Test Organizer";
    private WebsiteUrl websiteUrl = null;
    private LinkedHashSet<MemberId> coordinators = new LinkedHashSet<>();
    private RegistrationDeadlines registrationDeadlines = RegistrationDeadlines.none();
    private EventId eventId = new EventId(UUID.randomUUID());
    private Integer orisId = null;
    private EventTypeId eventTypeId = null;
    private List<EventRegistration> registrations = new ArrayList<>();
    private List<String> categories = List.of();
    private AuditMetadata auditMetadata = null;
    private EventRanking ranking = null;
    private Money baseEntryFee = null;

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
        this.coordinators = new LinkedHashSet<>();
        if (coordinatorId != null) {
            this.coordinators.add(coordinatorId);
        }
        return this;
    }

    public EventTestDataBuilder withCoordinators(LinkedHashSet<MemberId> coordinators) {
        this.coordinators = coordinators != null ? coordinators : new LinkedHashSet<>();
        return this;
    }

    public EventTestDataBuilder withRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadlines = registrationDeadline != null
                ? RegistrationDeadlines.single(registrationDeadline)
                : RegistrationDeadlines.none();
        return this;
    }

    public EventTestDataBuilder withRegistrationDeadlines(RegistrationDeadlines registrationDeadlines) {
        this.registrationDeadlines = registrationDeadlines != null ? registrationDeadlines : RegistrationDeadlines.none();
        return this;
    }

    public EventTestDataBuilder withOrisId(Integer orisId) {
        this.orisId = orisId;
        return this;
    }

    public EventTestDataBuilder withEventTypeId(EventTypeId eventTypeId) {
        this.eventTypeId = eventTypeId;
        return this;
    }

    public EventTestDataBuilder withCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public EventTestDataBuilder withRanking(EventRanking ranking) {
        this.ranking = ranking;
        return this;
    }

    public EventTestDataBuilder withBaseEntryFee(Money baseEntryFee) {
        this.baseEntryFee = baseEntryFee;
        return this;
    }

    public Event build() {
        return Event.reconstruct(eventId,
                name,
                eventDate,
                location,
                organizer,
                websiteUrl,
                coordinators,
                eventTypeId,
                registrationDeadlines,
                EventStatus.DRAFT,
                null,
                orisId,
                categories,
                ranking,
                baseEntryFee,
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
