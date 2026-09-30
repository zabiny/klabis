package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
class MemberAndLegalGuardianContactResolver implements GuardianContactResolver {

    private final MemberRepository memberRepository;
    private final LegalGuardianRepository legalGuardianRepository;

    MemberAndLegalGuardianContactResolver(MemberRepository memberRepository,
                                          LegalGuardianRepository legalGuardianRepository) {
        this.memberRepository = memberRepository;
        this.legalGuardianRepository = legalGuardianRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<GuardianContact> resolve(Collection<UserId> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<GuardianContact> contacts = new ArrayList<>();
        Set<UserId> resolved = new HashSet<>();

        for (Member member : memberRepository.findAllByIds(userIds.stream().map(MemberId::fromUserId).toList())) {
            if (!member.getPersonalInformation().isMinor()) {
                contacts.add(contactOf(member));
                resolved.add(member.getId().toUserId());
            }
        }
        List<UserId> remaining = userIds.stream().filter(id -> !resolved.contains(id)).distinct().toList();
        for (LegalGuardian guardian : legalGuardianRepository.findAllByIds(remaining)) {
            contacts.add(contactOf(guardian));
        }
        return contacts;
    }

    private static GuardianContact contactOf(Member member) {
        return new GuardianContact(member.getId().toUserId(), member.getFirstName(), member.getLastName(),
                member.getEmail() != null ? member.getEmail().value() : null,
                member.getPhone() != null ? member.getPhone().value() : null,
                GuardianKind.MEMBER);
    }

    private static GuardianContact contactOf(LegalGuardian guardian) {
        return new GuardianContact(guardian.getId(), guardian.getFirstName(), guardian.getLastName(),
                guardian.getEmail().value(), guardian.getPhone().value(), GuardianKind.LEGAL_GUARDIAN);
    }
}
