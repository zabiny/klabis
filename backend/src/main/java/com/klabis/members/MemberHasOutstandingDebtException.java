package com.klabis.members;

/**
 * Thrown when suspending a member who has a negative account balance (outstanding debt).
 * The caller must ensure the debt is resolved before re-attempting suspension.
 */
public class MemberHasOutstandingDebtException extends RuntimeException {

    private final MemberFinancialStatePort.MemberFinancialSnapshot snapshot;

    public MemberHasOutstandingDebtException(MemberFinancialStatePort.MemberFinancialSnapshot snapshot) {
        super("Member has an outstanding debt of %s %s — resolve before suspension"
                .formatted(snapshot.balance().amount().toPlainString(), snapshot.balance().currency()));
        this.snapshot = snapshot;
    }

    public MemberFinancialStatePort.MemberFinancialSnapshot getSnapshot() {
        return snapshot;
    }
}
