package com.klabis.members.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.common.users.application.ActivationContactVerifier;
import com.klabis.members.MemberId;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

@SecondaryAdapter
@Component
class MemberActivationContactVerifier implements ActivationContactVerifier {

    private final MemberRepository memberRepository;
    private final LegalGuardianRepository legalGuardianRepository;

    MemberActivationContactVerifier(MemberRepository memberRepository,
                                    LegalGuardianRepository legalGuardianRepository) {
        this.memberRepository = memberRepository;
        this.legalGuardianRepository = legalGuardianRepository;
    }

    @Override
    public boolean isActivationContact(UserId userId, String email) {
        if (email == null) {
            return false;
        }
        String candidate = email.trim();

        return memberRepository.findById(MemberId.fromUserId(userId))
                .map(member -> matchesMember(member, candidate))
                .orElseGet(() -> legalGuardianRepository.findById(userId)
                        .map(guardian -> matches(guardian.getEmail(), candidate))
                        .orElse(false));
    }

    // A minor never activates their account by themself; an admin does it for them (design D9).
    private boolean matchesMember(Member member, String candidate) {
        return !member.getPersonalInformation().isMinor() && matches(member.getEmail(), candidate);
    }

    private boolean matches(EmailAddress address, String candidate) {
        return address != null && address.value().trim().equalsIgnoreCase(candidate);
    }
}
