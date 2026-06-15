package com.klabis.members.application;

import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Set;

@PrimaryPort
public interface AllMembersPort {

    /**
     * Returns the IDs of all members, including suspended ones.
     * <p>
     * Suspended members are intentionally included: a reactivated member without a fee level choice
     * would otherwise be able to register for events without penalty. Including them in sanction
     * detection ensures the admin must perform an emergency assignment after reactivation.
     */
    Set<MemberId> findAll();
}
