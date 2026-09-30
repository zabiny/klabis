package com.klabis.members.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Derives which required details a member currently lacks (design.md D7 of {@code legal-guardians-via-groups}).
 * <p>
 * An adult needs their own e-mail and telephone and never misses a guardian. A minor's e-mail and
 * telephone are covered by their own or by those of any legal guardian, and a minor without a guardian
 * misses {@link MissingDataItem#GUARDIAN}. {@link MissingDataItem#BIRTH_NUMBER} is required only for Czech
 * nationals. Age is evaluated <i>today</i>, so a minor imported without a guardian becomes complete on
 * their 18th birthday without any write.
 */
public final class MemberCompleteness {

    private MemberCompleteness() {
    }

    public static Set<MissingDataItem> missingData(Member member, GuardianContacts guardians) {
        return missingData(member.getEmail(), member.getPhone(), member.getPersonalInformation(),
                member.getBirthNumber(), member.getAddress(), guardians);
    }

    public static Set<MissingDataItem> missingData(
            EmailAddress email,
            PhoneNumber phone,
            PersonalInformation personalInformation,
            BirthNumber birthNumber,
            Address address,
            GuardianContacts guardians) {

        Set<MissingDataItem> missing = EnumSet.noneOf(MissingDataItem.class);
        boolean minor = personalInformation != null && personalInformation.isMinor();

        if (address == null) {
            missing.add(MissingDataItem.ADDRESS);
        }
        if (email == null && !(minor && guardians.hasEmail())) {
            missing.add(MissingDataItem.EMAIL);
        }
        if (phone == null && !(minor && guardians.hasPhone())) {
            missing.add(MissingDataItem.PHONE);
        }
        if (personalInformation != null) {
            if (personalInformation.getNationality().isCzech() && birthNumber == null) {
                missing.add(MissingDataItem.BIRTH_NUMBER);
            }
            if (minor && !guardians.hasGuardian()) {
                missing.add(MissingDataItem.GUARDIAN);
            }
        }
        return missing;
    }
}
