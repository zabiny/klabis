package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventCategoryId;
import com.klabis.events.EventCategoryUpdate;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRanking;
import com.klabis.events.domain.EventUpdateEventBuilder;
import com.klabis.events.domain.SiPolicy;
import com.klabis.events.domain.SiUsage;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;

import static com.klabis.common.patch.PatchOverlay.overlay;
import static com.klabis.common.patch.PatchOverlay.overlayNonNull;

/**
 * Applies a PATCH {@link UpdateEventRequest} onto a fully pre-filled {@link Event.UpdateEvent}
 * baseline ({@link Event.UpdateEvent#from(Event)}). The controller holds the event to build that
 * baseline; authorization is declared on the generated {@link EventsApi}.
 * <p>
 * The three PATCH states are resolved here so the domain command never sees {@link JsonNullable}:
 * an <em>undefined</em> field leaves the baseline value in place, a <em>present-null</em> field
 * clears the (optional) target, and a <em>present</em> value sets it, as defined by
 * {@link com.klabis.common.patch.PatchOverlay}. {@code name}, {@code eventDate} and {@code organizer}
 * have no cleared state, so a present-null there retains the baseline.
 */
class UpdateEventRequestMapper {

    private UpdateEventRequestMapper() {}

    static Event.UpdateEvent toCommand(UpdateEventRequest request, Event.UpdateEvent prefilled) {
        var b = EventUpdateEventBuilder.builder(prefilled);

        overlayNonNull(request.name(), b::name);
        overlayNonNull(request.eventDate(), b::eventDate);
        overlayNonNull(request.organizer(), b::organizer);

        overlay(request.location(), b::location);
        overlay(request.websiteUrl(), b::websiteUrl);
        overlay(request.eventTypeId(), v -> b.eventTypeId(EventRequestConversions.toEventTypeId(v)));
        overlay(request.ranking(), v -> b.ranking(toRanking(v)));
        overlay(request.baseEntryFee(), v -> b.baseEntryFee(EventRequestConversions.toMoney(v)));

        overlay(request.sharedTransportEnabled(), b::sharedTransportEnabled);
        overlay(request.sharedAccommodationEnabled(), b::sharedAccommodationEnabled);

        overlaySiPolicy(request, prefilled.siPolicy(), b);

        overlay(request.coordinators(), v -> b.coordinators(EventRequestConversions.toCoordinators(v)));
        overlay(request.deadlines(), v -> b.registrationDeadlines(EventRequestConversions.toRegistrationDeadlines(v)));
        overlay(request.categories(), v -> b.categoryChanges(toCategoryChanges(v)));

        return b.build();
    }

    /**
     * Usage and rental are independent in the request but form one value object in the domain, so
     * each is resolved against the baseline before the policy is rebuilt. A present-null usage
     * keeps the current one, the rule of {@link com.klabis.common.patch.PatchOverlay#overlayNonNull}.
     */
    private static void overlaySiPolicy(UpdateEventRequest request, SiPolicy baseline,
                                        EventUpdateEventBuilder b) {
        SiUsage usage = request.siUsage().isPresent() && request.siUsage().get() != null
                ? SiUsage.valueOf(request.siUsage().get().name()) : baseline.usage();
        boolean rentalAllowed = request.siRentalAllowed().isPresent() && request.siRentalAllowed().get() != null
                ? request.siRentalAllowed().get() : baseline.rentalAllowed();
        b.siPolicy(SiPolicy.of(usage, rentalAllowed));
    }

    private static List<EventCategoryUpdate> toCategoryChanges(List<UpdateEventCategoryRequest> requested) {
        if (requested == null) {
            return List.of();
        }
        return requested.stream()
                .map(request -> new EventCategoryUpdate(
                        request.id() == null ? null : new EventCategoryId(request.id()),
                        request.name(),
                        EventRequestConversions.toMoney(request.fee())))
                .toList();
    }

    private static EventRanking toRanking(UpdateEventRankingRequest rankingRequest) {
        if (rankingRequest == null) {
            return null;
        }
        return EventRanking.of(rankingRequest.levelId(), rankingRequest.shortName(), rankingRequest.name());
    }
}
