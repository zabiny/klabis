package com.klabis.members.legalguardiangroup.domain;

import com.klabis.common.groups.domain.GroupFilter;
import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Set;

/**
 * Filter criteria for querying {@code LegalGuardianGroup} aggregates.
 * All fields are optional — null means no restriction on that dimension.
 *
 * @param guardianIs   groups in which the user is one of the guardians
 * @param minorIs      the group in which the user is a minor
 * @param guardiansAre groups whose guardians are exactly this set
 */
@ValueObject
public record LegalGuardianGroupFilter(
        UserId guardianIs,
        UserId minorIs,
        Set<UserId> guardiansAre
) implements GroupFilter {

    public static LegalGuardianGroupFilter all() {
        return new LegalGuardianGroupFilter(null, null, null);
    }

    public LegalGuardianGroupFilter withGuardianIs(UserId userId) {
        return new LegalGuardianGroupFilter(userId, minorIs, guardiansAre);
    }

    public LegalGuardianGroupFilter withMinorIs(UserId userId) {
        return new LegalGuardianGroupFilter(guardianIs, userId, guardiansAre);
    }

    public LegalGuardianGroupFilter withGuardiansAre(Set<UserId> guardians) {
        return new LegalGuardianGroupFilter(guardianIs, minorIs, guardians);
    }
}
