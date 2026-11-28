package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.members.domain.*;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

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
                        "Jan", "Novák", LocalDate.of(1990, 1, 15), Nationality.of("CZ"), Gender.MALE))
                .address(Address.of("Testovací 1", "Brno", "600 00", Country.of("CZ")))
                .email(EmailAddress.of("jan@example.com"))
                .phone(PhoneNumber.of("+420700000001"))
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
                .nationality(member.getNationality())
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
        @DisplayName("maps ORIS-owned fields into the projection")
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

        @DisplayName("ORIS gender code maps to the Gender enum, unrecognised codes to null")
        @ParameterizedTest(name = "gender \"{0}\" maps to {1}")
        @CsvSource(value = {"F,FEMALE", "X,null"}, nullValues = "null")
        void mapsGender(String orisGender, Gender expected) {
            var clubMember = referenceClubMember(b -> b.gender(orisGender));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.gender()).isEqualTo(expected);
        }

        @Test
        @DisplayName("si == 0 maps to no chip number, not \"0\"")
        void mapsZeroSiToNoChipNumber() {
            var clubMember = referenceClubMember(b -> b.si("0"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.chipNumber()).isNull();
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

        @DisplayName("an invalid ORIS value is dropped, logging the ORIS id and field but not the value")
        @ParameterizedTest(name = "{argumentSetName}")
        @MethodSource("invalidValuesDroppedAndLogged")
        void invalidValueIsDroppedAndLogged(UnaryOperator<ClubMemberBuilder> customizer,
                                            Function<MemberProjection, Object> droppedField,
                                            String loggedField,
                                            String rawValue,
                                            CapturedOutput output) {
            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(referenceClubMember(b -> customizer.apply(b.id(33630))));

            assertThat(droppedField.apply(projection)).isNull();
            assertThat(output).contains("33630").contains(loggedField);
            assertThat(output).doesNotContain(rawValue);
        }

        static Stream<Arguments> invalidValuesDroppedAndLogged() {
            return Stream.of(
                    dropped("malformed birth number", b -> b.persNum("not-a-birth-number"),
                            MemberProjection::birthNumber, "birthNumber", "not-a-birth-number"),
                    dropped("birth number of a non-Czech national", b -> b.nationality("SK").persNum("900115/0000"),
                            MemberProjection::birthNumber, "birthNumber", "900115/0000"),
                    dropped("unparsable phone number", b -> b.phone("+420abc123"),
                            MemberProjection::phone, "phone", "+420abc123"),
                    dropped("malformed e-mail", b -> b.email("not-an-email"),
                            MemberProjection::email, "email", "not-an-email"),
                    dropped("unknown nationality", b -> b.nationality("QQ"),
                            MemberProjection::nationality, "nationality", "QQ"));
        }

        private static Arguments dropped(String label,
                                         UnaryOperator<ClubMemberBuilder> customizer,
                                         Function<MemberProjection, Object> droppedField,
                                         String loggedField,
                                         String rawValue) {
            return argumentSet(label, customizer, droppedField, loggedField, rawValue);
        }

        @Test
        @DisplayName("an unknown address country is dropped with the whole address, logging the ORIS id and field but not the value")
        void unknownAddressCountryDropsAddressAndLogs(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.id(33630).country("QQ"));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.country()).isNull();
            assertThat(projection.street()).isNull();
            assertThat(projection.city()).isNull();
            assertThat(projection.postalCode()).isNull();
            assertThat(output).contains("33630").contains("country");
            assertThat(output).doesNotContain("QQ");
        }

        @Test
        @DisplayName("a partial address is dropped as a whole, including postal code and country, and logged with the ORIS id")
        void partialAddressIsClearedAsWhole(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.id(33630).city(" "));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.street()).isNull();
            assertThat(projection.city()).isNull();
            assertThat(projection.postalCode()).isNull();
            assertThat(projection.country()).isNull();
            assertThat(output).contains("33630").contains("dropping address");
        }

        @Test
        @DisplayName("an address with only the country present is cleared, keeping nationality and phone prefix intact")
        void addressWithOnlyCountryIsCleared() {
            var clubMember = referenceClubMember(b -> b.street(null).city(null).zip(null));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.street()).isNull();
            assertThat(projection.city()).isNull();
            assertThat(projection.postalCode()).isNull();
            assertThat(projection.country()).isNull();
            assertThat(projection.nationality()).isEqualTo("CZ");
            assertThat(projection.phone()).isEqualTo("+420700000001");
        }

        @Test
        @DisplayName("a fully absent address stays absent without any warning")
        void absentAddressIsNullWithoutWarning(CapturedOutput output) {
            var clubMember = referenceClubMember(b -> b.street(null).city(null).zip(null).country(null).phone(null));

            MemberProjection projection = MemberProjectionMapper.fromOrisClubMember(clubMember);

            assertThat(projection.street()).isNull();
            assertThat(projection.country()).isNull();
            assertThat(output).doesNotContain("dropping address");
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
                .nationality(member.getNationality())
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
