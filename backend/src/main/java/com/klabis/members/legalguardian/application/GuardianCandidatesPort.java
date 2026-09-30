package com.klabis.members.legalguardian.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface GuardianCandidatesPort {

    /**
     * Non-member legal guardians and active members aged 18 or more, each person once, whose name matches
     * every word of the query; everybody when the query is blank.
     */
    List<GuardianCandidate> findCandidates(String query);
}
