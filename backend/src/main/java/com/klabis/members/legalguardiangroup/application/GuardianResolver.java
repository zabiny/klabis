package com.klabis.members.legalguardiangroup.application;

import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Set;

@SecondaryPort
public interface GuardianResolver {

    /**
     * @throws GuardianNotFoundException when any of the users cannot serve as a guardian
     */
    Set<LegalGuardianGroup.Guardian> resolve(Set<UserId> userIds);
}
