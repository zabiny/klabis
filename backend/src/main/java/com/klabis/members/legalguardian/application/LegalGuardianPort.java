package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface LegalGuardianPort {

    record NewLegalGuardian(String firstName, String lastName, String email, String phone) {
    }

    /**
     * Creates the guardian together with an account that awaits activation, logging in with the next
     * EXTnnnn number.
     */
    LegalGuardianProfile register(NewLegalGuardian command);

    LegalGuardianProfile get(UserId id);

    LegalGuardianProfile update(UserId id, LegalGuardian.UpdateLegalGuardian command);
}
