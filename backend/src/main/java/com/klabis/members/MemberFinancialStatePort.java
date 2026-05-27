package com.klabis.members;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Secondary port owned by the members module. Defines what members need to know about a member's
 * financial state when deciding whether suspension is allowed.
 * Implemented by the finance module (dependency direction: finance → members).
 */
@SecondaryPort
public interface MemberFinancialStatePort {

    /**
     * Returns a snapshot of the member's financial state relevant for the suspend pre-check.
     * Returns a snapshot with zero balance and hasOutstandingDebt=false when no account exists
     * (e.g., during tests or early bootstrap before finance module is wired in).
     */
    MemberFinancialSnapshot getFinancialSnapshot(MemberId memberId);

    record MemberFinancialSnapshot(MemberId memberId, MonetaryAmount balance, boolean hasOutstandingDebt) {
    }
}
