package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.EventCategory;
import com.klabis.events.EventCategoryId;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class UpdateEventRequestMapper {

    private UpdateEventRequestMapper() {}

    /**
     * Merges a partial PATCH request with the current event state to produce a complete UpdateEvent command.
     * Fields absent from the request retain their current values from the event.
     * An explicitly provided null clears optional fields (e.g. eventTypeId, websiteUrl, location, ranking, baseEntryFee).
     */
    static Event.UpdateEvent toCommand(UpdateEventRequest request, Event existingEvent) {
        String name = request.name().orElse(existingEvent.getName());
        LocalDate eventDate = request.eventDate().orElse(existingEvent.getEventDate());
        String location = request.location().orElse(existingEvent.getLocation());
        String organizer = request.organizer().orElse(existingEvent.getOrganizer());
        String websiteUrl = request.websiteUrl().orElse(
                existingEvent.getWebsiteUrl() != null ? existingEvent.getWebsiteUrl().value() : null);
        LinkedHashSet<MemberId> coordinators = request.coordinators().isPresent()
                ? EventRequestConversions.toCoordinators(request.coordinators().orElseThrow())
                : new LinkedHashSet<>(existingEvent.getCoordinators());
        EventTypeId eventTypeId = request.eventTypeId().isPresent()
                ? EventRequestConversions.toEventTypeId(request.eventTypeId().orElseThrow())
                : existingEvent.getEventTypeId().orElse(null);
        RegistrationDeadlines registrationDeadlines = request.deadlines().isPresent()
                ? EventRequestConversions.toRegistrationDeadlines(request.deadlines().orElseThrow())
                : existingEvent.getRegistrationDeadlines();
        List<EventCategory> categories = request.categories().isPresent()
                ? toCategories(request.categories().orElseThrow(), existingEvent)
                : existingEvent.getCategories();

        EventRanking ranking = request.ranking().map(UpdateEventRequestMapper::toRanking).orElse(existingEvent.getRanking());
        Money baseEntryFee = request.baseEntryFee().map(EventRequestConversions::toMoney).orElse(existingEvent.getBaseEntryFee());

        return new Event.UpdateEvent(name, eventDate, location, organizer, websiteUrl,
                coordinators, eventTypeId, registrationDeadlines, categories, ranking, baseEntryFee);
    }

    /**
     * Applies the id / no-id / missing-id semantics from the design: a request category carrying
     * an {@code id} must reference one already on this event (updates name/fee, id and any
     * registration links are preserved); a request category without an {@code id} is new and gets
     * a freshly generated one; any existing category id absent from the request is dropped.
     */
    private static List<EventCategory> toCategories(List<UpdateEventCategoryRequest> requested, Event existingEvent) {
        if (requested == null) {
            return List.of();
        }
        Map<EventCategoryId, EventCategory> existingById = existingEvent.getCategories().stream()
                .collect(Collectors.toMap(EventCategory::id, c -> c));

        return requested.stream()
                .map(request -> {
                    if (request.id() == null) {
                        return new EventCategory(EventCategoryId.generate(), null, request.name(), EventRequestConversions.toMoney(request.fee()));
                    }
                    EventCategoryId id = new EventCategoryId(request.id());
                    EventCategory existing = existingById.get(id);
                    if (existing == null) {
                        throw new BusinessRuleViolationException(
                                "Category id '" + id + "' does not belong to this event") {};
                    }
                    return new EventCategory(existing.id(), existing.orisId(), request.name(), EventRequestConversions.toMoney(request.fee()));
                })
                .toList();
    }

    private static EventRanking toRanking(UpdateEventRankingRequest rankingRequest) {
        if (rankingRequest == null) {
            return null;
        }
        return EventRanking.of(rankingRequest.levelId(), rankingRequest.shortName(), rankingRequest.name());
    }

}
