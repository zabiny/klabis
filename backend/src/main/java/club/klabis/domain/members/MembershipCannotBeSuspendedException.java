package club.klabis.domain.members;

public class MembershipCannotBeSuspendedException extends RuntimeException {
    public MembershipCannotBeSuspendedException(Member.Id memberId, String reason) {
        super("Membership for member %d cannot be suspended - %s".formatted(memberId.value(), reason));
    }
}
