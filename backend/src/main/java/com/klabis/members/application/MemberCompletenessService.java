package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberCompleteness;
import com.klabis.members.domain.MissingDataItem;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public GuardianContacts guardianContactsOf(Member member) {
        if (!member.getPersonalInformation().isMinor()) {
            return GuardianContacts.NONE;
        }
        return contactsOfGuardians(legalGuardianGroupPort.guardiansOf(member.getId()));
    }

    @Transactional(readOnly = true)
    @Override
    public GuardianContacts contactsOfChosenGuardians(Set<UserId> guardians) {
        if (guardians.isEmpty()) {
            return GuardianContacts.NONE;
        }
        List<GuardianContact> contacts = guardianContactResolver.resolve(guardians);
        Set<UserId> usable = contacts.stream().map(GuardianContact::userId).collect(Collectors.toSet());
        guardians.stream().filter(id -> !usable.contains(id)).findFirst().ifPresent(id -> {
            throw new GuardianNotFoundException(id);
        });
        return toGuardianContacts(contacts);
    }

    private GuardianContacts contactsOfGuardians(Set<UserId> guardians) {
        if (guardians.isEmpty()) {
            return GuardianContacts.NONE;
        }
        return toGuardianContacts(guardianContactResolver.resolve(guardians));
    }

    private static GuardianContacts toGuardianContacts(List<GuardianContact> contacts) {
        return new GuardianContacts(true,
                contacts.stream().anyMatch(contact -> hasText(contact.email())),
                contacts.stream().anyMatch(contact -> hasText(contact.phone())));
    }

    @Transactional(readOnly = true)
    @Override
    public Set<MissingDataItem> missingData(Member member) {
        return MemberCompleteness.missingData(member, guardianContactsOf(member));
    }

    @Transactional(readOnly = true)
    @Override
    public Set<MissingDataItem> missingData(Member member, Set<UserId> guardians) {
        GuardianContacts contacts = member.getPersonalInformation().isMinor()
                                    ? contactsOfGuardians(guardians)
                                    : GuardianContacts.NONE;
        return MemberCompleteness.missingData(member, contacts);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
