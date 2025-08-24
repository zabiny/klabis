package club.klabis.members.domain;

import club.klabis.members.MemberId;

public class MembershipCannotBeSuspendedException extends RuntimeException {
    public MembershipCannotBeSuspendedException(MemberId memberId, String reason) {
        super("Membership for member %d cannot be suspended - %s".formatted(memberId.value(), reason));
    }
}
