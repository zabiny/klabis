package club.klabis.domain.members.events;

import club.klabis.domain.members.Member;

public class MemberWasSuspendedEvent extends DomainEvent<Member> {
    public MemberWasSuspendedEvent(Member suspendedMember) {
        super(suspendedMember);
    }
}
