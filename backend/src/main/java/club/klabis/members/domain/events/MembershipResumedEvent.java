package club.klabis.members.domain.events;

import club.klabis.members.domain.Member;
import club.klabis.shared.domain.AggregateDomainEvent;

public class MembershipResumedEvent extends AggregateDomainEvent<Member> {
    public MembershipResumedEvent(Member member) {
        super(member);
    }
}
