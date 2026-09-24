package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.members.domain.*;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberProjectionMapper")
@ExtendWith(OutputCaptureExtension.class)
class MemberProjectionMapperTest {

    private static ClubMember referenceClubMember() {
        return referenceClubMember(UnaryOperator.identity());
    }

    private static ClubMember referenceClubMember(UnaryOperator<ClubMemberBuilder> customizer) {
        ClubMemberBuilder builder = ClubMemberBuilder.builder()
                .id(33630)
                .userId(49207)
                .regNum("ZBM0001")
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("testuser1")
                .firstName("Jan")
                .lastName("Novák")
                .email("jan@example.com")
                .street("Testovací 1")
                .city("Brno")
                .zip("600 00")
                .country("CZ")
                .birthday(LocalDate.of(1990, 1, 15))
                .phone("700000001")
                .gender("M")
                .persNum("900115/0000")
                .nationality("CZ")
                .si("1000001");
        return customizer.apply(builder).build();
    }

    private static Member referenceMember() {
        Member member = Member.register(MemberRegisterMemberBuilder.builder()
                .id(new com.klabis.members.MemberId(UUID.randomUUID()))
                .registrationNumber(RegistrationNumber.of("ZBM0001"))
                .personalInformation(PersonalInformation.of(
                        "Jan", "Novák", LocalDate.of(1990, 1, 15), "CZ", Gender.MALE))
                .address(Address.of("Testovací 1", "Brno", "600 00", "CZ"))
                .email(EmailAddress.of("jan@example.com"))
                .phone(PhoneNumber.of("+420700000001"))
                .guardian(null)
                .birthNumber(BirthNumber.of("900115/0000"))
                .bankAccountNumber(null)
                .registeredBy(null)
                .build());
        member.syncFromOris(MemberSyncFromOrisBuilder.builder()
                .registrationNumber(member.getRegistrationNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .dateOfBirth(member.getDateOfBirth())
                .gender(member.getGender())
                .nationality(Nationality.of(member.getNationality()))
                .birthNumber(member.getBirthNumber())
                .email(member.getEmail())
                .phone(member.getPhone())
                .address(member.getAddress())
                .chipNumber("1000001")
                .build());
        return member;
    }

    @Nested
    @DisplayName("fromOrisClubMember()")
    class FromOrisClubMember {

        @Test
        @DisplayName("maps ORIS-owned fields into the projection (design.md D6)")
        void mapsOrisFields() {
            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(referenceClubMember());

            assertThat(projection.registrationNumber()).isEqualTo("ZBM0001");
            assertThat(projection.firstName()).isEqualTo("Jan");
            assertThat(projection.lastName()).isEqualTo("Novák");
            assertThat(projection.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(projection.gender()).isEqualTo(Gender.MALE);
            assertThat(projection.nationality()).isEqualTo("CZ");
            assertThat(projection.birthNumber()).isEqualTo("900115/0000");
            assertThat(projection.email()).isEqualTo("jan@example.com");
            assertThat(projection.phone()).isEqualTo("+420700000001");
            assertThat(projection.street()).isEqualTo("Testovací 1");
            assertThat(projection.city()).isEqualTo("Brno");
            assertThat(projection.postalCode()).isEqualTo("600 00");
            assertThat(projection.country()).isEqualTo("CZ");
            assertThat(projection.chipNumber()).isEqualTo("1000001");
        }

        @Test
        @DisplayName("gender \"F\" maps to FEMALE")
        void mapsFemaleGender() {
            var clubMember = referenceClubMember(b -> b.gender("F"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.gender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("an unrecognised gender value maps to null")
        void mapsUnknownGenderToNull() {
            var clubMember = referenceClubMember(b -> b.gender("X"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.gender()).isNull();
        }

        @Test
        @DisplayName("si == 0 maps chip number to null, not \"0\"")
        void mapsZeroSiToNullChipNumber() {
            var clubMember = referenceClubMember(b -> b.si("0"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.chipNumber()).isNull();
        }

        @Test
        @DisplayName("a non-zero si maps to its decimal string form")
        void mapsNonZeroSiToDecimalString() {
            var clubMember = referenceClubMember(b -> b.si("1000001"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.chipNumber()).isEqualTo("1000001");
        }

        @Test
        @DisplayName("blank optional text fields map to null")
        void mapsBlankFieldsToNull() {
            var clubMember = referenceClubMember(b -> b.email("").street("  ").city(""));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.email()).isNull();
            assertThat(projection.street()).isNull();
            assertThat(projection.city()).isNull();
        }

        @Test
        @DisplayName("birth number passes through unchanged (RRMMDD/XXXX)")
        void birthNumberPassesThroughUnchanged() {
            var clubMember = referenceClubMember(b -> b.persNum("900115/0000"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.birthNumber()).isEqualTo("900115/0000");
        }

        @Test
        @DisplayName("a postal code with an inner space passes through unchanged")
        void postalCodeWithInnerSpacePassesThroughUnchanged() {
            var clubMember = referenceClubMember(b -> b.zip("600 00"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.postalCode()).isEqualTo("600 00");
        }

        @Test
        @DisplayName("nationality is upper-cased defensively")
        void nationalityIsUpperCased() {
            var clubMember = referenceClubMember(b -> b.nationality("cz"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.nationality()).isEqualTo("CZ");
        }

        @Test
        @DisplayName("phone prefix comes from the postal country, not nationality, when they differ")
        void phonePrefixComesFromCountryNotNationality() {
            var clubMember = referenceClubMember(b -> b.nationality("SK").country("CZ").phone("700000001"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.nationality()).isEqualTo("SK");
            assertThat(projection.country()).isEqualTo("CZ");
            assertThat(projection.phone()).isEqualTo("+420700000001");
        }

        @Test
        @DisplayName("a malformed birth number is dropped, logging the ORIS id and field but not the value (design.md D6)")
        void malformedBirthNumberIsDroppedAndLogged(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.id(33630).persNum("not-a-birth-number"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.birthNumber()).isNull();
            assertThat(output).contains("33630").contains("birthNumber");
            assertThat(output).doesNotContain("not-a-birth-number");
        }

        @Test
        @DisplayName("a birth number for a non-Czech national is dropped, logging the ORIS id and field but not the value (design.md D6)")
        void birthNumberForNonCzechNationalIsDroppedAndLogged(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.id(33630).nationality("SK").persNum("900115/0000"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.birthNumber()).isNull();
            assertThat(output).contains("33630").contains("birthNumber");
            assertThat(output).doesNotContain("900115/0000");
        }

        @Test
        @DisplayName("an unparsable ORIS phone number is dropped, logging the ORIS id and field but not the value (design.md D6)")
        void invalidPhoneIsDroppedAndLogged(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.id(33630).phone("+420abc123"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.phone()).isNull();
            assertThat(output).contains("33630").contains("phone");
            assertThat(output).doesNotContain("+420abc123");
        }

        @Test
        @DisplayName("a malformed ORIS e-mail is dropped, logging the ORIS id and field but not the value (design.md D6)")
        void invalidEmailIsDroppedAndLogged(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.id(33630).email("not-an-email"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.email()).isNull();
            assertThat(output).contains("33630").contains("email");
            assertThat(output).doesNotContain("not-an-email");
        }

        @Test
        @DisplayName("a missing birth number is left null without any warning")
        void missingBirthNumberIsNullWithoutWarning(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.persNum(null));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.birthNumber()).isNull();
            assertThat(output).doesNotContain("birthNumber");
        }
    }

    @Nested
    @DisplayName("fromMember()")
    class FromMember {

        @Test
        @DisplayName("maps a local Member's ORIS-owned fields into the projection")
        void mapsMemberFields() {
            MemberProjection projection = MemberProjectionMapper.fromMember(referenceMember());

            assertThat(projection.registrationNumber()).isEqualTo("ZBM0001");
            assertThat(projection.firstName()).isEqualTo("Jan");
            assertThat(projection.lastName()).isEqualTo("Novák");
            assertThat(projection.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(projection.gender()).isEqualTo(Gender.MALE);
            assertThat(projection.nationality()).isEqualTo("CZ");
            assertThat(projection.birthNumber()).isEqualTo("900115/0000");
            assertThat(projection.email()).isEqualTo("jan@example.com");
            assertThat(projection.phone()).isEqualTo("+420700000001");
            assertThat(projection.street()).isEqualTo("Testovací 1");
            assertThat(projection.city()).isEqualTo("Brno");
            assertThat(projection.postalCode()).isEqualTo("600 00");
            assertThat(projection.country()).isEqualTo("CZ");
            assertThat(projection.chipNumber()).isEqualTo("1000001");
        }
    }

    @Test
    @DisplayName("mapping the ORIS side and the local side of equal data hashes equally")
    void fromOrisClubMember_andFromMember_withEqualData_hashEqually() {
        MemberProjection fromOris = MemberProjectionMapper.fromOrisClubMember(referenceClubMember());
        MemberProjection fromLocal = MemberProjectionMapper.fromMember(referenceMember());

        assertThat(SyncProjectionCodec.hash(fromOris)).isEqualTo(SyncProjectionCodec.hash(fromLocal));
    }

    @Test
    @DisplayName("mapping the same ORIS data twice produces an identical hash — no phantom differences from si/blank-string handling")
    void fromOrisClubMember_mappedTwice_hashesIdentically() {
        var clubMember = referenceClubMember(b -> b.email("").si("0"));

        MemberProjection first = MemberProjectionMapper.fromOrisClubMember(clubMember);
        MemberProjection second = MemberProjectionMapper.fromOrisClubMember(clubMember);

        assertThat(SyncProjectionCodec.hash(first)).isEqualTo(SyncProjectionCodec.hash(second));
    }

    @Test
    @DisplayName("no chip number hashes the same on both sides — si=0 is not a phantom difference from a Klabis-side null")
    void fromOrisClubMember_andFromMember_withNoChipNumber_hashEqually() {
        var clubMember = referenceClubMember(b -> b.si("0"));
        MemberProjection fromOris = MemberProjectionMapper.fromOrisClubMember(clubMember);

        Member member = referenceMember();
        member.syncFromOris(MemberSyncFromOrisBuilder.builder()
                .registrationNumber(member.getRegistrationNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .dateOfBirth(member.getDateOfBirth())
                .gender(member.getGender())
                .nationality(Nationality.of(member.getNationality()))
                .birthNumber(member.getBirthNumber())
                .email(member.getEmail())
                .phone(member.getPhone())
                .address(member.getAddress())
                .chipNumber(null)
                .build());
        MemberProjection fromLocal = MemberProjectionMapper.fromMember(member);

        assertThat(SyncProjectionCodec.hash(fromOris)).isEqualTo(SyncProjectionCodec.hash(fromLocal));
    }
}
