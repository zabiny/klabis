package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wire-to-domain conversions shared by the create and update event mappers.
 *
 * <p>These used to be methods on the hand-written request records. The records are now generated
 * from the OpenAPI spec and carry wire types only ({@code UUID} rather than {@code MemberId}), so
 * the conversions live here instead — see the "DTOs carry wire types" rule in the klabis-api-spec
 * skill.
 */
final class EventRequestConversions {

    private EventRequestConversions() {}

    /**
     * Absent coordinators become an empty set rather than null: the command's field is a plain
     * {@code LinkedHashSet}, and the domain reads "no coordinators" from emptiness.
     */
    static LinkedHashSet<MemberId> toCoordinators(Collection<UUID> coordinators) {
        if (coordinators == null) {
            return new LinkedHashSet<>();
        }
        return coordinators.stream().map(MemberId::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static EventTypeId toEventTypeId(UUID eventTypeId) {
        return eventTypeId == null ? null : new EventTypeId(eventTypeId);
    }

    static Money toMoney(EntryFeeRequest fee) {
        return fee == null ? null : Money.of(fee.amount(), Money.parseCurrency(fee.currency()));
    }

    /**
     * The wire carries deadlines as a flat list; the domain models them as up to three named
     * positions. Ordering is enforced separately by {@link DeadlinesOrdered}.
     */
    static RegistrationDeadlines toRegistrationDeadlines(List<LocalDate> deadlines) {
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
