package com.klabis.members.legalguardiangroup.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;
import java.util.Set;

@PrimaryPort
public interface LegalGuardianGroupPort {

    List<LegalGuardianGroup> listGroups();

    LegalGuardianGroup getGroup(LegalGuardianGroupId id);

    /**
     * Guardians of the whole group change at once; the group is edited in place, or merged into the
     * group that already has exactly these guardians.
     */
    void changeGroupGuardians(LegalGuardianGroupId id, Set<UserId> guardians);

    /**
     * Guardians of a single minor change; siblings keep theirs. The minor moves to the group with exactly
     * these guardians, which is created when there is none.
     */
    void setGuardiansOf(MemberId minor, Set<UserId> guardians);

    /**
     * Like {@link #changeGroupGuardians(LegalGuardianGroupId, Set)}, for guardians as entered in a form: new
     * guardians are created in the same transaction.
     */
    void changeGroupGuardians(LegalGuardianGroupId id, List<GuardianInput> guardians);

    /**
     * Like {@link #setGuardiansOf(MemberId, Set)}, for guardians as entered in a form: new guardians are
     * created in the same transaction.
     */
    void setGuardiansOf(MemberId minor, List<GuardianInput> guardians);

    Set<UserId> guardiansOf(MemberId minor);

    /**
     * Contacts of the group's guardians, whether they are members or non-member guardians.
     */
    List<GuardianContact> listGuardians(LegalGuardianGroupId id);
}
