package com.klabis.members;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

/**
 * Read access to non-member legal guardians for other modules.
 */
@PrimaryPort
public interface LegalGuardians {

    Optional<LegalGuardianDto> findById(UserId userId);
}
