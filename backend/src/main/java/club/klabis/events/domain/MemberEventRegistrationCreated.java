package club.klabis.events.domain;

import club.klabis.members.MemberId;
import club.klabis.shared.domain.AggregateDomainEvent;

public class MemberEventRegistrationCreated extends AggregateDomainEvent<Event> {
    private final MemberId memberId;

    protected MemberEventRegistrationCreated(Event aggregate, MemberId memberId) {
        super(aggregate);
        this.memberId = memberId;
    }
}
