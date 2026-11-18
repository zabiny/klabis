package com.klabis.members.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;

import java.util.List;

/**
 * Thrown when suspending a member is blocked by one or both independent reasons: the member
 * still owns groups that need a successor, and/or the member has an outstanding debt. Both
 * checks always run, so both blockers are reported together in one response instead of forcing
 * the caller to fix one, resubmit, and only then discover the other.
 */
public class SuspensionBlockedException extends ConflictException implements UserFacingError {

    private final List<OwnedGroup> blockingGroups;
    private final MemberFinancialStatePort.MemberFinancialSnapshot debtSnapshot;
    private final List<DependentBlockers> dependents;

    public SuspensionBlockedException(List<OwnedGroup> blockingGroups,
                                       MemberFinancialStatePort.MemberFinancialSnapshot debtSnapshot) {
        this(blockingGroups, debtSnapshot, List.of());
    }

    public SuspensionBlockedException(List<OwnedGroup> blockingGroups,
                                       MemberFinancialStatePort.MemberFinancialSnapshot debtSnapshot,
                                       List<DependentBlockers> dependents) {
        super("Suspension blocked: %d blocking group(s), outstandingDebt=%s, %d blocked dependent member(s)"
                .formatted(blockingGroups.size(), debtSnapshot != null, dependents.size()));
        this.blockingGroups = blockingGroups;
        this.debtSnapshot = debtSnapshot;
        this.dependents = dependents;
    }

    /**
     * Blockers of a member suspended together with the requested one (whole legal guardian group).
     *
     * @param debtSnapshot null when the member has no outstanding debt
     */
    public record DependentBlockers(MemberId memberId, String memberName, List<OwnedGroup> blockingGroups,
                                    MemberFinancialStatePort.MemberFinancialSnapshot debtSnapshot) {
    }

    public List<DependentBlockers> getDependents() {
        return dependents;
    }

    public List<OwnedGroup> getBlockingGroups() {
        return blockingGroups;
    }

    public MemberFinancialStatePort.MemberFinancialSnapshot getDebtSnapshot() {
        return debtSnapshot;
    }
}
