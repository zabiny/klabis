package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

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
        MemberId eventCoordinatorId = request.eventCoordinatorId().patchValue(existingEvent.getEventCoordinatorId());
        EventTypeId eventTypeId = request.eventTypeId().patchValue(existingEvent.getEventTypeId().orElse(null));
        RegistrationDeadlines registrationDeadlines = request.deadlines().isProvided()
                ? toRegistrationDeadlines(request.deadlines().throwIfNotProvided())
                : existingEvent.getRegistrationDeadlines();
        List<String> categories = request.categories().patchValue(existingEvent.getCategories());

        EventRanking ranking = request.ranking().isProvided()
                ? toRanking(request.ranking().throwIfNotProvided())
                : existingEvent.getRanking();
        Money baseEntryFee = request.baseEntryFee().isProvided()
                ? toMoney(request.baseEntryFee().throwIfNotProvided())
                : existingEvent.getBaseEntryFee();

        return new Event.UpdateEvent(name, eventDate, location, organizer, websiteUrl,
                eventCoordinatorId, eventTypeId, registrationDeadlines, categories, ranking, baseEntryFee);
    }

    private static EventRanking toRanking(UpdateEventRequest.RankingRequest rankingRequest) {
        if (rankingRequest == null) {
            return null;
        }
        return EventRanking.of(rankingRequest.levelId(), rankingRequest.shortName(), rankingRequest.name());
    }

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("CZK");

    private static Money toMoney(UpdateEventRequest.EntryFeeRequest feeRequest) {
        if (feeRequest == null) {
            return null;
        }
        Currency currency;
        try {
            currency = Currency.getInstance(feeRequest.currency());
        } catch (IllegalArgumentException e) {
            currency = DEFAULT_CURRENCY;
        }
        return Money.of(feeRequest.amount(), currency);
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
