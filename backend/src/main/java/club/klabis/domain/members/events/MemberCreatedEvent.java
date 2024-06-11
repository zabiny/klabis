package club.klabis.domain.members.events;

import club.klabis.domain.members.Member;

public class MemberCreatedEvent extends DomainEvent<Member>  {

    public MemberCreatedEvent(Member aggregate) {
        super(aggregate);
    }
}
