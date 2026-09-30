package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MissingDataItem;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Set;

@PrimaryPort
public interface MemberCompletenessPort {

    /**
     * What the current legal guardians of the member offer towards the member's completeness; nothing for an adult,
     * whose completeness never depends on guardians.
     */
    GuardianContacts guardianContactsOf(Member member);

    /**
     * What the given legal guardians offer towards the completeness of a member who is not yet saved.
     *
     * @throws com.klabis.members.legalguardiangroup.application.GuardianNotFoundException when one of the chosen
     *                                                                                      guardians cannot serve as a guardian
     */
    GuardianContacts contactsOfChosenGuardians(Set<UserId> guardians);

    /**
     * Live completeness of the member, taking the current legal guardians and their contacts into account.
     */
    Set<MissingDataItem> missingData(Member member);

    /**
     * Like {@link #missingData(Member)} for a caller that has already looked up the member's current legal guardians.
     */
    Set<MissingDataItem> missingData(Member member, Set<UserId> guardians);
}
