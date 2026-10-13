package com.klabis.events.infrastructure;

import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.events.domain.EventRepository;
import com.klabis.members.MemberId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An event coordinator may edit the event and read its registrations in full. Publishing and cancelling stay
 * with EVENTS:MANAGE, which is why coordinators hold narrower authorities rather than EVENTS:MANAGE itself.
 */
@Component
class EventCoordinatorRelationshipSource implements RelationshipSource {

    private final EventRepository eventRepository;

    EventCoordinatorRelationshipSource(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        Set<TargetRef> coordinated = eventRepository.findIdsCoordinatedBy(MemberId.fromUserId(userId)).stream()
                .map(eventId -> TargetRef.event(eventId.value()))
                .collect(Collectors.toUnmodifiableSet());
        if (coordinated.isEmpty()) {
            return Map.of();
        }
        return Map.of(
                Authority.EVENTS_EDIT, coordinated,
                Authority.EVENTS_REGISTRATIONS_READ, coordinated);
    }
}
