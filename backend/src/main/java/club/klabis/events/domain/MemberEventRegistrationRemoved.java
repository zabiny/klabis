package club.klabis.events.domain;

import club.klabis.members.MemberId;
import club.klabis.shared.domain.AggregateDomainEvent;

public class MemberEventRegistrationRemoved extends AggregateDomainEvent<Event> {
    private final MemberId memberId;

    protected MemberEventRegistrationRemoved(Event aggregate, MemberId memberId) {
        super(aggregate);
        this.memberId = memberId;
    }

    public MemberId getMemberId() {
        return memberId;
    }
}
