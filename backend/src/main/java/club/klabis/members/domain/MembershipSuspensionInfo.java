package club.klabis.members.domain;

public record MembershipSuspensionInfo(Member member, DetailStatus financeAccount) {

    public boolean canSuspend() {
        return !member().isSuspended() && financeAccount().canSuspend();
    }

    public void assertSuspendAccountPossible() {
        if (!financeAccount().canSuspend()) {
            throw new MembershipCannotBeSuspendedException(member().getId(),
                    "Member ma nevyrizene financni zalezitosti");
        }
    }

    public static enum DetailStatus {
        OK, PREVENTING_SUSPENSION;

        public boolean canSuspend() {
            return OK.equals(this);
        }
    }
}
