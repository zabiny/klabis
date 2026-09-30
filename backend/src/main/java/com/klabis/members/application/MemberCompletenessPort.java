package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MissingDataItem;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Set;

@PrimaryPort
public interface MemberCompletenessPort {

    /**
     * What the current legal guardians of the member offer towards the member's completeness.
     */
    GuardianContacts guardianContactsOf(MemberId memberId);

    /**
     * What the given legal guardians offer towards the completeness of a member who is not yet saved.
     */
    GuardianContacts contactsOfGuardians(Set<UserId> guardians);

    /**
     * Live completeness of the member, taking the current legal guardians and their contacts into account.
     */
    Set<MissingDataItem> missingData(Member member);
}
