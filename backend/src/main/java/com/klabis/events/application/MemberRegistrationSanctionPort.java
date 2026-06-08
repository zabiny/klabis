package com.klabis.events.application;

import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface MemberRegistrationSanctionPort {

    /**
     * Applies a sanction for a member who missed the fee selection deadline:
     * blocks the member from new event registrations and auto-unregisters them
     * from all events with currently open registrations.
     * <p>
     * The set of auto-unregistered registrations is logged at WARNING level to allow
     * manual restoration after an emergency assignment resolves the sanction.
     */
    void applyMissedSelectionSanction(MemberId memberId);

    /**
     * Returns true when the member is currently blocked from new event registrations
     * due to a missed fee selection.
     */
    boolean isMemberBlocked(MemberId memberId);
}
