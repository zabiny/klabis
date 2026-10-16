package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
class MemberContactService implements MemberContactPort {

    private static final Comparator<GuardianContact> BY_NAME = Comparator
            .comparing(GuardianContact::lastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(GuardianContact::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final LegalGuardianGroupPort legalGuardianGroupPort;
    private final GuardianContactResolver guardianContactResolver;

    MemberContactService(LegalGuardianGroupPort legalGuardianGroupPort,
                         GuardianContactResolver guardianContactResolver) {
        this.legalGuardianGroupPort = legalGuardianGroupPort;
        this.guardianContactResolver = guardianContactResolver;
    }

    @Transactional(readOnly = true)
    @Override
    public EffectiveContact effectiveContactOf(Member member) {
        if (!needsGuardian(member)) {
            return ownContactOf(member);
        }
        return effectiveContactOf(member, legalGuardianGroupPort.guardiansOf(member.getId()));
    }

    @Transactional(readOnly = true)
    @Override
    public EffectiveContact effectiveContactOf(Member member, Set<UserId> guardians) {
        if (guardians.isEmpty() || !needsGuardian(member)) {
            return ownContactOf(member);
        }
        List<GuardianContact> ordered = guardianContactResolver.resolve(guardians).stream().sorted(BY_NAME).toList();
        return new EffectiveContact(
                resolve(ownEmail(member), ordered, GuardianContact::email),
                resolve(ownPhone(member), ordered, GuardianContact::phone));
    }

    private static boolean needsGuardian(Member member) {
        return member.getPersonalInformation().isMinor()
               && (!StringUtils.hasText(ownEmail(member)) || !StringUtils.hasText(ownPhone(member)));
    }

    private static EffectiveContact ownContactOf(Member member) {
        return new EffectiveContact(resolve(ownEmail(member), List.of(), GuardianContact::email),
                resolve(ownPhone(member), List.of(), GuardianContact::phone));
    }

    private static EffectiveContact.Value resolve(String ownValue, List<GuardianContact> ordered,
                                                  Function<GuardianContact, String> detail) {
        if (StringUtils.hasText(ownValue)) {
            return new EffectiveContact.Value(ownValue, null);
        }
        return ordered.stream()
                .filter(guardian -> StringUtils.hasText(detail.apply(guardian)))
                .findFirst()
                .map(guardian -> new EffectiveContact.Value(detail.apply(guardian),
                        "%s %s".formatted(guardian.firstName(), guardian.lastName())))
                .orElse(EffectiveContact.Value.EMPTY);
    }

    private static String ownEmail(Member member) {
        return Optional.ofNullable(member.getEmail()).map(EmailAddress::value).orElse(null);
    }

    private static String ownPhone(Member member) {
        return Optional.ofNullable(member.getPhone()).map(PhoneNumber::value).orElse(null);
    }
}
