package com.klabis.members.application;

import com.klabis.members.MemberId;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Set;

/**
 * Secondary port owned by the members module: lets a suspension of a legal guardian take the minors of the
 * guardian's groups along. Implemented by the legal guardian group module.
 */
@SecondaryPort
public interface LegalGuardianCascadePort {

    /**
     * Minors of the given groups.
     *
     * @throws NotSoleGuardianGroupException when one of the groups does not exist or the guardian is not its
     *                                       sole guardian (a group with another guardian never needs a cascade)
     */
    List<MemberId> minorsOfSoleGuardianGroups(MemberId guardian, Set<LegalGuardianGroupId> groupIds);
}
