package club.klabis.domain.members;

public record MembershipSuspensionInfo(boolean isMemberSuspended, boolean financeAccountCleared) {

    public boolean canSuspendAccount() {
        return !isMemberSuspended() && financeAccountCleared();  // if additional conditions apply, add them here
    }

}
