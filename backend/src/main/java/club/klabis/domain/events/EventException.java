package club.klabis.domain.events;

import club.klabis.domain.members.Member;

public class EventException extends RuntimeException {
    private final Event.Id eventId;
    private final Type type;

    enum Type {
        MEMBER_ALREADY_REGISTERED, MEMBER_NOT_REGISTERED
    }

    public static EventException createAlreadySignedUpException(Event.Id eventId, Member.Id memberId) {
        return new EventException(eventId,
                "Member with ID '%s' is already signed up to event with ID '%s'".formatted(memberId.value(),
                        eventId.value()),
                Type.MEMBER_ALREADY_REGISTERED);
    }

    public static EventException createMemberNotRegisteredForEventException(Event.Id eventId, Member.Id memberId) {
        return new EventException(eventId,
                "Member with ID '%s' is NOT registered to event with ID '%s'".formatted(memberId.value(),
                        eventId.value()),
                Type.MEMBER_NOT_REGISTERED);
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
