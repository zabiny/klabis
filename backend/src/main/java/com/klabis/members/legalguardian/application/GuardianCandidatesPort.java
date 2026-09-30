package com.klabis.members.legalguardian.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;

import java.util.List;

@PrimaryPort
public interface GuardianCandidatesPort {

    /**
     * Non-member legal guardians and active members aged 18 or more, each person once, labelled for display.
     * Only the given kind of candidates is returned when {@code kind} is set.
     */
    List<GuardianCandidate> findCandidates(@Nullable GuardianKind kind);
}
