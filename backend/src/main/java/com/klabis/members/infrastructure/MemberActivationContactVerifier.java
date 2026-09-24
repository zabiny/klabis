package com.klabis.members.infrastructure;

import com.klabis.common.users.application.ActivationContactVerifier;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.GuardianInformation;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

@SecondaryAdapter
@Component
class MemberActivationContactVerifier implements ActivationContactVerifier {

    private final MemberRepository memberRepository;

    MemberActivationContactVerifier(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean isActivationContact(String registrationNumber, String email) {
        if (!RegistrationNumber.isRegistrationNumber(registrationNumber) || email == null) {
            return false;
        }

        return memberRepository.findByRegistrationNumber(RegistrationNumber.of(registrationNumber))
                .map(member -> matches(member, email))
                .orElse(false);
    }

    private boolean matches(Member member, String email) {
        String candidate = email.trim();

        if (matches(member.getEmail(), candidate)) {
            return true;
        }

        GuardianInformation guardian = member.getGuardian();
        return guardian != null && matches(guardian.getEmail(), candidate);
    }

    private boolean matches(EmailAddress address, String candidate) {
        return address != null && address.value().trim().equalsIgnoreCase(candidate);
    }
}
