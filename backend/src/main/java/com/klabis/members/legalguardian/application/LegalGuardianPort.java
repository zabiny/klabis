package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Set;

@PrimaryPort
public interface LegalGuardianPort {

    record NewLegalGuardian(String firstName, String lastName, String email, String phone) {
    }

    /**
     * A guardian as entered in a form: either an existing one chosen by user id or a new non-member guardian.
     */
    record GuardianInput(UserId userId, NewLegalGuardian newGuardian) {

        public GuardianInput {
            Assert.isTrue((userId == null) != (newGuardian == null),
                    "Legal guardian must be either a chosen user or a new guardian");
        }

        public static GuardianInput existing(UserId userId) {
            return new GuardianInput(userId, null);
        }

        public static GuardianInput created(NewLegalGuardian newGuardian) {
            return new GuardianInput(null, newGuardian);
        }
    }

    /**
     * Creates the guardian together with an account that awaits activation, logging in with the next
     * EXTnnnn number.
     */
    LegalGuardianProfile register(NewLegalGuardian command);

    /**
     * Turns form input into user ids, registering the new guardians on the way. Runs in the caller's
     * transaction, so a failure anywhere leaves no new guardian behind.
     */
    Set<UserId> resolveGuardians(List<GuardianInput> inputs);

    /**
     * Removes the guardian profile of a guardian who is being registered as a club member. The user account
     * stays, so the person keeps their login number and their guardian groups.
     */
    void releaseForMembership(UserId id);

    LegalGuardianProfile get(UserId id);

    LegalGuardianProfile update(UserId id, LegalGuardian.UpdateLegalGuardian command);
}
