package club.klabis.domain.members.events;

import club.klabis.domain.DomainEvent;
import club.klabis.domain.members.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberEditedEvent extends DomainEvent<Member> {

    public MemberEditedEvent(Member aggregate) {
        super(aggregate);
    }
}
