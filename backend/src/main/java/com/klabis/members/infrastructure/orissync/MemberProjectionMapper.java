package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.PhoneNumber;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps both sides of a {@code Member} synchronisation into the canonical
 * {@link MemberProjection} shape (design.md D2, D6).
 * <p>
 * Blank strings from either side are normalised to {@code null}, so that "absent" has
 * one representation on both sides of the hash (design.md D6). {@code si == 0} maps to
 * {@code null}, not {@code "0"}: chip number zero does not exist, and treating it as a
 * value would make "no chip" and "chip 0" hash differently.
 */
final class MemberProjectionMapper {

    private static final Logger log = LoggerFactory.getLogger(MemberProjectionMapper.class);

    private MemberProjectionMapper() {
    }

    static MemberProjection fromOrisClubMember(ClubMember clubMember) {
        String country = normalizeNationality(clubMember.country());
        String nationality = normalizeNationality(clubMember.nationality());
        String phone = mapPhone(clubMember.id(), PhoneNumberNormalizer.normalize(clubMember.phone(), country));

        String street = blankToNull(clubMember.street());
        String city = blankToNull(clubMember.city());
        String postalCode = blankToNull(clubMember.zip());
        String addressCountry = country;
        if (isPartialAddress(street, city, postalCode, addressCountry)) {
            log.warn("ORIS member {}: dropping address - incomplete", clubMember.id());
            street = null;
            city = null;
            postalCode = null;
            addressCountry = null;
        }

        return new MemberProjection(
                clubMember.regNum(),
                blankToNull(clubMember.firstName()),
                blankToNull(clubMember.lastName()),
                clubMember.birthday(),
                mapGender(clubMember.gender()),
                nationality,
                mapBirthNumber(clubMember.id(), clubMember.persNum(), nationality),
                mapEmail(clubMember.id(), blankToNull(clubMember.email())),
                phone,
                street,
                city,
                postalCode,
                addressCountry,
                mapChipNumber(clubMember.si())
        );
    }

    /**
     * Klabis's {@link Address} is all-or-nothing (design.md ADDRESS): a member either has a
     * complete postal address or none at all. An ORIS record with some but not all of the four
     * components is treated the same as one with none, since a half-built {@link Address} cannot
     * be constructed anyway.
     */
    private static boolean isPartialAddress(String street, String city, String postalCode, String country) {
        boolean anyPresent = street != null || city != null || postalCode != null || country != null;
        boolean allPresent = street != null && city != null && postalCode != null && country != null;
        return anyPresent && !allPresent;
    }

    static MemberProjection fromMember(Member member) {
        Address address = member.getAddress();
        return new MemberProjection(
                member.getRegistrationNumber() != null ? member.getRegistrationNumber().getValue() : null,
                blankToNull(member.getFirstName()),
                blankToNull(member.getLastName()),
                member.getDateOfBirth(),
                member.getGender(),
                normalizeNationality(member.getNationality()),
                member.getBirthNumber() != null ? member.getBirthNumber().value() : null,
                member.getEmail() != null ? member.getEmail().value() : null,
                member.getPhone() != null ? member.getPhone().value() : null,
                address != null ? blankToNull(address.street()) : null,
                address != null ? blankToNull(address.city()) : null,
                address != null ? blankToNull(address.postalCode()) : null,
                address != null ? normalizeNationality(address.country()) : null,
                blankToNull(member.getChipNumber())
        );
    }

    /**
     * Leaves out a birth number Klabis cannot accept, rather than refusing the whole member
     * (design.md D6): a value for a non-Czech national, or one in an invalid format. Each drop is
     * logged at WARN with the ORIS id and the field name only — never the value, since it is
     * personal data.
     */
    private static String mapBirthNumber(int orisId, String rawBirthNumber, String nationality) {
        String value = blankToNull(rawBirthNumber);
        if (value == null) {
            return null;
        }

        if (!"CZ".equals(nationality)) {
            log.warn("ORIS member {}: dropping birthNumber - not allowed for a non-Czech national", orisId);
            return null;
        }

        try {
            BirthNumber.of(value);
            return value;
        } catch (IllegalArgumentException e) {
            log.warn("ORIS member {}: dropping birthNumber - invalid format", orisId);
            return null;
        }
    }

    /**
     * Leaves out an ORIS phone number Klabis cannot accept (design.md D6), validating through
     * {@link PhoneNumber} itself rather than duplicating its format rules here. Logged at WARN
     * with the ORIS id and the field name only — never the value.
     */
    private static String mapPhone(int orisId, String normalizedPhone) {
        if (normalizedPhone == null) {
            return null;
        }
        try {
            PhoneNumber.of(normalizedPhone);
            return normalizedPhone;
        } catch (IllegalArgumentException e) {
            log.warn("ORIS member {}: dropping phone - invalid format", orisId);
            return null;
        }
    }

    /**
     * Leaves out an ORIS e-mail address Klabis cannot accept (design.md D6), validating through
     * {@link EmailAddress} itself rather than duplicating its format rules here. Logged at WARN
     * with the ORIS id and the field name only — never the value.
     */
    private static String mapEmail(int orisId, String rawEmail) {
        if (rawEmail == null) {
            return null;
        }
        try {
            EmailAddress.of(rawEmail);
            return rawEmail;
        } catch (IllegalArgumentException e) {
            log.warn("ORIS member {}: dropping email - invalid format", orisId);
            return null;
        }
    }

    private static Gender mapGender(String orisGender) {
        if ("M".equals(orisGender)) {
            return Gender.MALE;
        }
        if ("F".equals(orisGender)) {
            return Gender.FEMALE;
        }
        return null;
    }

    private static String mapChipNumber(String si) {
        return (StringUtils.isBlank(si) || "0".equals(si)) ? null : si;
    }

    private static String normalizeNationality(String code) {
        String value = blankToNull(code);
        return value != null ? value.toUpperCase() : null;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
