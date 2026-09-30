package com.klabis.members.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.LocalDate;

@PrimaryPort
public interface MemberAgeOutPort {

    /**
     * Re-evaluates completeness of members who turn 18 on the given day and announces that they are no longer minors.
     */
    void processMembersComingOfAge(LocalDate today);
}
