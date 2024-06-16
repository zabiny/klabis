package club.klabis.domain.members.events;

import club.klabis.domain.members.Member;

public class MemberEditedEvent extends DomainEvent<Member>  {

    public MemberEditedEvent(Member aggregate) {
        super(aggregate);
    }
}
