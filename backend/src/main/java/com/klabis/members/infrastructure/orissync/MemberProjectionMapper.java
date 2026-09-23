package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import org.apache.commons.lang3.StringUtils;

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

    private MemberProjectionMapper() {
    }

    static MemberProjection fromOrisClubMember(ClubMember clubMember) {
        String country = normalizeNationality(clubMember.country());
        return new MemberProjection(
                clubMember.regNum(),
                blankToNull(clubMember.firstName()),
                blankToNull(clubMember.lastName()),
                clubMember.birthday(),
                mapGender(clubMember.gender()),
                normalizeNationality(clubMember.nationality()),
                blankToNull(clubMember.persNum()),
                blankToNull(clubMember.email()),
                PhoneNumberNormalizer.normalize(clubMember.phone(), country),
                blankToNull(clubMember.street()),
                blankToNull(clubMember.city()),
                blankToNull(clubMember.zip()),
                country,
                mapChipNumber(clubMember.si())
        );
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
