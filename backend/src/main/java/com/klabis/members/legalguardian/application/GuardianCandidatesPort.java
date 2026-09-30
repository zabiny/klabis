package com.klabis.members.legalguardian.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;

import java.util.List;

@PrimaryPort
public interface GuardianCandidatesPort {

    /**
     * Non-member legal guardians and active members aged 18 or more, each person once, whose name matches
     * every word of the query; everybody when the query is blank. Only the given kind of candidates is
     * returned when {@code kind} is set.
     */
    List<GuardianCandidate> findCandidates(String query, @Nullable GuardianKind kind);
}
