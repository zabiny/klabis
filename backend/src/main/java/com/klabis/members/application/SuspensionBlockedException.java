package com.klabis.members.application;

import com.klabis.members.OwnedGroup;

import java.util.List;

/**
 * Thrown when suspending a member is blocked by one or both independent reasons: the member
 * still owns groups that need a successor, and/or the member has an outstanding debt. Both
 * checks always run, so both blockers are reported together in one response instead of forcing
 * the caller to fix one, resubmit, and only then discover the other.
 */
public class SuspensionBlockedException extends RuntimeException {

    private final List<OwnedGroup> blockingGroups;
    private final MemberFinancialStatePort.MemberFinancialSnapshot debtSnapshot;

    public SuspensionBlockedException(List<OwnedGroup> blockingGroups,
                                       MemberFinancialStatePort.MemberFinancialSnapshot debtSnapshot) {
        super("Suspension blocked: %d blocking group(s), outstandingDebt=%s"
                .formatted(blockingGroups.size(), debtSnapshot != null));
        this.blockingGroups = blockingGroups;
        this.debtSnapshot = debtSnapshot;
    }

    public List<OwnedGroup> getBlockingGroups() {
        return blockingGroups;
    }

    public MemberFinancialStatePort.MemberFinancialSnapshot getDebtSnapshot() {
        return debtSnapshot;
    }
}
