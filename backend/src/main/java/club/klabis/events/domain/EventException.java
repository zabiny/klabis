package club.klabis.events.domain;

import club.klabis.members.MemberId;

import java.util.Set;
import java.util.stream.Collectors;

public class EventException extends RuntimeException {
    private final Event.Id eventId;
    private final Type type;

    public enum Type {
        MEMBER_ALREADY_REGISTERED, MEMBER_NOT_REGISTERED, EVENT_NOT_FOUND, REGISTRATION_DEADLINE_PASSED, BLOCKED_CATEGORIES, UNSPECIFIED
    }

    public static EventException createAlreadySignedUpException(Event.Id eventId, MemberId memberId) {
        return new EventException(eventId,
                "Member with ID '%s' is already signed up to event with ID '%s'".formatted(memberId.value(),
                        eventId.value()),
                Type.MEMBER_ALREADY_REGISTERED);
    }

    public static EventException createMemberNotRegisteredForEventException(Event.Id eventId, MemberId memberId) {
        return new EventException(eventId,
                "Member with ID '%s' is NOT registered to event with ID '%s'".formatted(memberId.value(),
                        eventId.value()),
                Type.MEMBER_NOT_REGISTERED);
    }

    public static EventException createEventNotFoundException(Event.Id eventId) {
        return new EventException(eventId,
                "Event with ID '%s' was not found".formatted(eventId.value()),
                Type.EVENT_NOT_FOUND);
    }

    public static EventException createCategoriesUpdateRejectedException(Event.Id eventId, Set<Competition.Category> blockedCategories) {
        return new EventException(eventId,
                "Cannot remove categories because they are used in registrations. Blocked categories: " + blockedCategories.stream()
                        .map(
                                Competition.Category::name)
                        .collect(Collectors.joining(", ")),
                Type.BLOCKED_CATEGORIES);
    }

    public EventException(Event.Id eventId, String message, Type type) {
        super(message);
        this.eventId = eventId;
        this.type = type;
    }

    public Event.Id getEventId() {
        return eventId;
    }

    public Type getType() {
        return type;
    }
}
