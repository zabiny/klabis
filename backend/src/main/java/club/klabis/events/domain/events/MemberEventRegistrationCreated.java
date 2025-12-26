package club.klabis.events.domain.events;

import club.klabis.events.domain.Event;
import club.klabis.members.MemberId;
import club.klabis.shared.domain.AggregateDomainEvent;

public class MemberEventRegistrationCreated extends AggregateDomainEvent<Event> {
    private final MemberId memberId;

    public MemberEventRegistrationCreated(Event aggregate, MemberId memberId) {
        super(aggregate);
        this.memberId = memberId;
    }

    public MemberId getMemberId() {
        return memberId;
    }
}
