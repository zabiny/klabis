package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberCompleteness;
import com.klabis.members.domain.MissingDataItem;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
class MemberCompletenessService implements MemberCompletenessPort {

    private final LegalGuardianGroupPort legalGuardianGroupPort;
    private final GuardianContactResolver guardianContactResolver;

    MemberCompletenessService(LegalGuardianGroupPort legalGuardianGroupPort,
                              GuardianContactResolver guardianContactResolver) {
        this.legalGuardianGroupPort = legalGuardianGroupPort;
        this.guardianContactResolver = guardianContactResolver;
    }

    @Transactional(readOnly = true)
    @Override
    public GuardianContacts guardianContactsOf(MemberId memberId) {
        return contactsOfGuardians(legalGuardianGroupPort.guardiansOf(memberId));
    }

    @Transactional(readOnly = true)
    @Override
    public GuardianContacts contactsOfGuardians(Set<UserId> guardians) {
        if (guardians.isEmpty()) {
            return GuardianContacts.NONE;
        }
        List<GuardianContact> contacts = guardianContactResolver.resolve(guardians);
        return new GuardianContacts(true,
                contacts.stream().anyMatch(contact -> hasText(contact.email())),
                contacts.stream().anyMatch(contact -> hasText(contact.phone())));
    }

    @Transactional(readOnly = true)
    @Override
    public Set<MissingDataItem> missingData(Member member) {
        return MemberCompleteness.missingData(member, guardianContactsOf(member.getId()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
