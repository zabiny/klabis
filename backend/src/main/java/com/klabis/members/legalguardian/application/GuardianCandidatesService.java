package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
class GuardianCandidatesService implements GuardianCandidatesPort {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Collator CZECH = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));

    private final LegalGuardianRepository legalGuardianRepository;
    private final MemberRepository memberRepository;

    GuardianCandidatesService(LegalGuardianRepository legalGuardianRepository, MemberRepository memberRepository) {
        this.legalGuardianRepository = legalGuardianRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<GuardianCandidate> findCandidates(String query) {
        Map<UserId, GuardianCandidate> byUser = new LinkedHashMap<>();

        for (LegalGuardian guardian : legalGuardianRepository.findAll()) {
            if (matches(query, guardian.getFirstName(), guardian.getLastName())) {
                byUser.put(guardian.getId(), new GuardianCandidate(guardian.getId(),
                        guardian.getName().fullName(), GuardianKind.LEGAL_GUARDIAN, null,
                        guardian.getEmail().value()));
            }
        }
        for (Member member : memberRepository.findAll(MemberFilter.activeOnly().withFulltext(query))) {
            if (!member.getPersonalInformation().isMinor()) {
                byUser.put(member.getId().toUserId(), new GuardianCandidate(member.getId().toUserId(),
                        member.getPersonalInformation().getName().fullName(), GuardianKind.MEMBER,
                        member.getRegistrationNumber().getValue(),
                        member.getEmail() != null ? member.getEmail().value() : null));
            }
        }

        List<GuardianCandidate> candidates = new ArrayList<>(byUser.values());
        candidates.sort(Comparator.comparing(GuardianCandidate::displayName, CZECH));
        return candidates;
    }

    private static boolean matches(String query, String firstName, String lastName) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String haystack = normalize(firstName + " " + lastName);
        for (String token : normalize(query).split("\\s+")) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String text) {
        return DIACRITICS.matcher(Normalizer.normalize(text.trim(), Normalizer.Form.NFD)).replaceAll("")
                .toLowerCase(Locale.ROOT);
    }
}
