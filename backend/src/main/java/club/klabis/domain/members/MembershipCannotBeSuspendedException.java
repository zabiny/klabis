package club.klabis.domain.members;

public class MembershipCannotBeSuspendedException extends RuntimeException {
    public MembershipCannotBeSuspendedException(int memberId, String reason) {
        super("Membership for member %d cannot be suspended - %s".formatted(memberId, reason));
    }
}
