package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
class GuardianCandidatesService implements GuardianCandidatesPort {

    private static final Collator CZECH = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));

    private final LegalGuardianRepository legalGuardianRepository;
    private final MemberRepository memberRepository;

    GuardianCandidatesService(LegalGuardianRepository legalGuardianRepository, MemberRepository memberRepository) {
        this.legalGuardianRepository = legalGuardianRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<GuardianCandidate> findCandidates(@Nullable GuardianKind kind) {
        Map<UserId, GuardianCandidate> byUser = new LinkedHashMap<>();

        if (kind != GuardianKind.MEMBER) {
            addLegalGuardians(byUser);
        }
        if (kind != GuardianKind.LEGAL_GUARDIAN) {
            addAdultMembers(byUser);
        }

        List<GuardianCandidate> candidates = new ArrayList<>(byUser.values());
        candidates.sort(Comparator.comparing(GuardianCandidate::displayName, CZECH));
        return candidates;
    }

    private void addLegalGuardians(Map<UserId, GuardianCandidate> byUser) {
        for (LegalGuardian guardian : legalGuardianRepository.findAll()) {
            String email = guardian.getEmail().value();
            byUser.put(guardian.getId(), new GuardianCandidate(guardian.getId(),
                    "%s (%s)".formatted(guardian.getName().fullName(), email), GuardianKind.LEGAL_GUARDIAN, null, email));
        }
    }

    private void addAdultMembers(Map<UserId, GuardianCandidate> byUser) {
        MemberFilter adultsOnly = MemberFilter.activeOnly()
                .withBornOnOrBefore(LocalDate.now().minusYears(PersonalInformation.ADULT_AGE));
        for (Member member : memberRepository.findAll(adultsOnly)) {
            String registrationNumber = member.getRegistrationNumber().getValue();
            byUser.put(member.getId().toUserId(), new GuardianCandidate(member.getId().toUserId(),
                    "%s (%s)".formatted(member.getPersonalInformation().getName().fullName(), registrationNumber),
                    GuardianKind.MEMBER, registrationNumber,
                    member.getEmail() != null ? member.getEmail().value() : null));
        }
    }
}
