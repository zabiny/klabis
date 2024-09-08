package club.klabis.domain.members;

public record MembershipSuspensionInfo(boolean isMemberSuspended, DetailStatus financeAccount) {

    public boolean canSuspendAccount() {
        return !isMemberSuspended() && financeAccount().canSuspend();  // if additional conditions apply, add them here
    }

    public static enum DetailStatus {
        OK, PREVENTING_SUSPENSION;

        public boolean canSuspend() {
            return OK.equals(this);
        }
    }
}
