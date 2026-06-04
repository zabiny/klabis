package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.List;

class UpdateEventRequestMapper {

    private UpdateEventRequestMapper() {}

    /**
     * Merges a partial PATCH request with the current event state to produce a complete UpdateEvent command.
     * Fields absent from the request retain their current values from the event.
     * An explicitly provided null clears optional fields (e.g. eventTypeId, websiteUrl, location).
     */
    static Event.UpdateEvent toCommand(UpdateEventRequest request, Event existingEvent) {
        String name = request.name().patchValue(existingEvent.getName());
        LocalDate eventDate = request.eventDate().patchValue(existingEvent.getEventDate());
        String location = request.location().patchValue(existingEvent.getLocation());
        String organizer = request.organizer().patchValue(existingEvent.getOrganizer());
        String websiteUrl = request.websiteUrl().patchValue(
                existingEvent.getWebsiteUrl() != null ? existingEvent.getWebsiteUrl().value() : null);
        MemberId eventCoordinatorId = request.eventCoordinatorId().patchValue(existingEvent.getEventCoordinatorId());
        EventTypeId eventTypeId = request.eventTypeId().patchValue(existingEvent.getEventTypeId().orElse(null));
        RegistrationDeadlines registrationDeadlines = request.deadlines().isProvided()
                ? toRegistrationDeadlines(request.deadlines().throwIfNotProvided())
                : existingEvent.getRegistrationDeadlines();
        List<String> categories = request.categories().patchValue(existingEvent.getCategories());

        return new Event.UpdateEvent(name, eventDate, location, organizer, websiteUrl,
                eventCoordinatorId, eventTypeId, registrationDeadlines, categories,
                existingEvent.getRanking(), existingEvent.getBaseEntryFee());
    }

    private static RegistrationDeadlines toRegistrationDeadlines(List<LocalDate> deadlines) {
        if (deadlines == null || deadlines.isEmpty()) {
            return RegistrationDeadlines.none();
        }
        return RegistrationDeadlines.of(
                deadlines.get(0),
                deadlines.size() > 1 ? deadlines.get(1) : null,
                deadlines.size() > 2 ? deadlines.get(2) : null
        );
    }
}
