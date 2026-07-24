package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCategory;
import com.klabis.events.domain.EventCategoryId;
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
        String name = request.name().patchValue(existingEvent.getName());
        LocalDate eventDate = request.eventDate().patchValue(existingEvent.getEventDate());
        String location = request.location().patchValue(existingEvent.getLocation());
        String organizer = request.organizer().patchValue(existingEvent.getOrganizer());
        String websiteUrl = request.websiteUrl().patchValue(
                existingEvent.getWebsiteUrl() != null ? existingEvent.getWebsiteUrl().value() : null);
        LinkedHashSet<MemberId> coordinators = request.coordinators().patchValue(new LinkedHashSet<>(existingEvent.getCoordinators()));
        EventTypeId eventTypeId = request.eventTypeId().patchValue(existingEvent.getEventTypeId().orElse(null));
        RegistrationDeadlines registrationDeadlines = request.deadlines().isProvided()
                ? toRegistrationDeadlines(request.deadlines().throwIfNotProvided())
                : existingEvent.getRegistrationDeadlines();
        List<EventCategory> categories = request.categories().isProvided()
                ? toCategories(request.categories().throwIfNotProvided(), existingEvent)
                : existingEvent.getCategories();

        EventRanking ranking = request.ranking().map(UpdateEventRequestMapper::toRanking).patchValue(existingEvent.getRanking());
        Money baseEntryFee = request.baseEntryFee().map(UpdateEventRequestMapper::toMoney).patchValue(existingEvent.getBaseEntryFee());

        return new Event.UpdateEvent(name, eventDate, location, organizer, websiteUrl,
                coordinators, eventTypeId, registrationDeadlines, categories, ranking, baseEntryFee);
    }

    /**
     * Applies the id / no-id / missing-id semantics from the design: a request category carrying
     * an {@code id} must reference one already on this event (updates name/fee, id and any
     * registration links are preserved); a request category without an {@code id} is new and gets
     * a freshly generated one; any existing category id absent from the request is dropped.
     */
    private static List<EventCategory> toCategories(List<UpdateEventRequest.CategoryRequest> requested, Event existingEvent) {
        if (requested == null) {
            return List.of();
        }
        Map<EventCategoryId, EventCategory> existingById = existingEvent.getCategories().stream()
                .collect(Collectors.toMap(EventCategory::id, c -> c));

        return requested.stream()
                .map(request -> {
                    if (request.id() == null) {
                        return new EventCategory(EventCategoryId.generate(), null, request.name(), toMoney(request.fee()));
                    }
                    EventCategory existing = existingById.get(request.id());
                    if (existing == null) {
                        throw new BusinessRuleViolationException(
                                "Category id '" + request.id() + "' does not belong to this event") {};
                    }
                    return new EventCategory(existing.id(), existing.orisId(), request.name(), toMoney(request.fee()));
                })
                .toList();
    }

    private static EventRanking toRanking(UpdateEventRequest.RankingRequest rankingRequest) {
        if (rankingRequest == null) {
            return null;
        }
        return EventRanking.of(rankingRequest.levelId(), rankingRequest.shortName(), rankingRequest.name());
    }

    private static Money toMoney(UpdateEventRequest.EntryFeeRequest feeRequest) {
        if (feeRequest == null) {
            return null;
        }
        return Money.of(feeRequest.amount(), Money.parseCurrency(feeRequest.currency()));
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
