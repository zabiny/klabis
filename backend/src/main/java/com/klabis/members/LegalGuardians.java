package com.klabis.members;

import com.klabis.common.users.UserId;

import java.util.Optional;

/**
 * Read access to non-member legal guardians for other modules.
 */
public interface LegalGuardians {

    Optional<LegalGuardianDto> findById(UserId userId);
}
