package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BankAccountNumber;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.IdentityCard;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberUpdateMemberBuilder;
import com.klabis.members.domain.MedicalCourse;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.domain.RefereeLevel;
import com.klabis.members.domain.RefereeLicense;
import com.klabis.members.domain.TrainerLevel;
import com.klabis.members.domain.TrainerLicense;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateMemberRequestMapper")
class UpdateMemberRequestMapperTest {

    private static final UserId EDITOR = new UserId(UUID.randomUUID());
    private static final LocalDate TODAY = LocalDate.now();

    private final Member.UpdateMember baseline = MemberUpdateMemberBuilder.builder()
            .email(EmailAddress.of("base@example.com"))
            .phone(PhoneNumber.of("+420111222333"))
            .address(Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")))
            .chipNumber("111")
            .nationality(Nationality.of("CZ"))
            .bankAccountNumber(BankAccountNumber.of("12345/5678"))
            .identityCard(IdentityCard.of("BASE123", TODAY.plusYears(5)))
            .drivingLicenseGroup(com.klabis.members.domain.DrivingLicenseGroup.B)
            .medicalCourse(MedicalCourse.of(TODAY.minusYears(1), Optional.of(TODAY.plusYears(1))))
            .trainerLicense(TrainerLicense.of(TrainerLevel.T1, TODAY.plusYears(2)))
            .refereeLicense(RefereeLicense.of(RefereeLevel.R1, TODAY.plusYears(2)))
            .dietaryRestrictions("none")
            .firstName("Jan")
            .lastName("Novák")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .gender(Gender.MALE)
            .birthNumber(BirthNumber.of("900515/1234"))
            .build();

    private static UpdateMemberRequestBuilder undefinedRequest() {
        return UpdateMemberRequestBuilder.builder()
                .email(JsonNullable.undefined()).phone(JsonNullable.undefined()).address(JsonNullable.undefined())
                .firstName(JsonNullable.undefined()).lastName(JsonNullable.undefined()).dateOfBirth(JsonNullable.undefined()).gender(JsonNullable.undefined())
                .chipNumber(JsonNullable.undefined()).nationality(JsonNullable.undefined())
                .identityCard(JsonNullable.undefined()).medicalCourse(JsonNullable.undefined()).trainerLicense(JsonNullable.undefined())
                .refereeLicense(JsonNullable.undefined()).drivingLicenseGroup(JsonNullable.undefined())
                .dietaryRestrictions(JsonNullable.undefined()).birthNumber(JsonNullable.undefined()).bankAccountNumber(JsonNullable.undefined());
    }

    @Test
    @DisplayName("maps every request field onto the command")
    void shouldMapAllFields() {
        LocalDate validity = TODAY.plusYears(3);
        var request = undefinedRequest()
                .email(JsonNullable.of("new@example.com"))
                .phone(JsonNullable.of("+420999888777"))
                .address(JsonNullable.of(new AddressRequest("Nová 5", "Brno", "60200", "SK")))
                .firstName(JsonNullable.of("Petr"))
                .lastName(JsonNullable.of("Svoboda"))
                .dateOfBirth(JsonNullable.of(LocalDate.of(1985, 3, 20)))
                .gender(JsonNullable.of(UpdateMemberRequestGender.FEMALE))
                .chipNumber(JsonNullable.of("12345"))
                .nationality(JsonNullable.of("SK"))
                .identityCard(JsonNullable.of(new IdentityCardDto("NEW987", validity)))
                .medicalCourse(JsonNullable.of(new MedicalCourseDto(LocalDate.of(2024, 1, 1), validity)))
                .trainerLicense(JsonNullable.of(new TrainerLicenseDto(TrainerLicenseDtoLevel.T2, validity)))
                .refereeLicense(JsonNullable.of(new RefereeLicenseDto(RefereeLicenseDtoLevel.R3, validity)))
                .drivingLicenseGroup(JsonNullable.of(com.klabis.members.infrastructure.restapi.DrivingLicenseGroup.A))
                .dietaryRestrictions(JsonNullable.of("Vegan"))
                .birthNumber(JsonNullable.of("850520/9876"))
                .bankAccountNumber(JsonNullable.of("98765/4321"))
                .build();

        Member.UpdateMember command = UpdateMemberRequestMapper.toCommand(request, baseline, EDITOR);

        assertThat(command.email()).isEqualTo(EmailAddress.of("new@example.com"));
        assertThat(command.phone()).isEqualTo(PhoneNumber.of("+420999888777"));
        assertThat(command.address()).isEqualTo(Address.of("Nová 5", "Brno", "60200", Country.of("SK")));
        assertThat(command.firstName()).isEqualTo("Petr");
        assertThat(command.lastName()).isEqualTo("Svoboda");
        assertThat(command.dateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 20));
        assertThat(command.gender()).isEqualTo(Gender.FEMALE);
        assertThat(command.chipNumber()).isEqualTo("12345");
        assertThat(command.nationality()).isEqualTo(Nationality.of("SK"));
        assertThat(command.identityCard()).isEqualTo(IdentityCard.of("NEW987", validity));
        assertThat(command.medicalCourse())
                .isEqualTo(MedicalCourse.of(LocalDate.of(2024, 1, 1), Optional.of(validity)));
        assertThat(command.trainerLicense()).isEqualTo(TrainerLicense.of(TrainerLevel.T2, validity));
        assertThat(command.refereeLicense()).isEqualTo(RefereeLicense.of(RefereeLevel.R3, validity));
        assertThat(command.drivingLicenseGroup()).isEqualTo(com.klabis.members.domain.DrivingLicenseGroup.A);
        assertThat(command.dietaryRestrictions()).isEqualTo("Vegan");
        assertThat(command.birthNumber()).isEqualTo(BirthNumber.of("850520/9876"));
        assertThat(command.bankAccountNumber()).isEqualTo(BankAccountNumber.of("98765/4321"));
        assertThat(command.updatedBy()).isEqualTo(EDITOR);
    }

    @Test
    @DisplayName("keeps the baseline value of every field the request leaves undefined")
    void shouldKeepBaselineForUndefinedFields() {
        Member.UpdateMember command = UpdateMemberRequestMapper.toCommand(undefinedRequest().build(), baseline, EDITOR);

        assertThat(command).isEqualTo(MemberUpdateMemberBuilder.builder(baseline).updatedBy(EDITOR).build());
    }

    @Test
    @DisplayName("an explicit null clears the clearable fields and leaves fields without a cleared state alone")
    void shouldClearOnExplicitNull() {
        var request = undefinedRequest()
                .email(JsonNullable.of(null))
                .phone(JsonNullable.of(null))
                .address(JsonNullable.of(null))
                .chipNumber(JsonNullable.of(null))
                .identityCard(JsonNullable.of(null))
                .medicalCourse(JsonNullable.of(null))
                .trainerLicense(JsonNullable.of(null))
                .refereeLicense(JsonNullable.of(null))
                .drivingLicenseGroup(JsonNullable.of(null))
                .dietaryRestrictions(JsonNullable.of(null))
                .birthNumber(JsonNullable.of(null))
                .bankAccountNumber(JsonNullable.of(null))
                .firstName(JsonNullable.of(null))
                .lastName(JsonNullable.of(null))
                .dateOfBirth(JsonNullable.of(null))
                .gender(JsonNullable.of(null))
                .nationality(JsonNullable.of(null))
                .build();

        Member.UpdateMember command = UpdateMemberRequestMapper.toCommand(request, baseline, EDITOR);

        assertThat(command.email()).isNull();
        assertThat(command.phone()).isNull();
        assertThat(command.address()).isNull();
        assertThat(command.chipNumber()).isNull();
        assertThat(command.identityCard()).isNull();
        assertThat(command.medicalCourse()).isNull();
        assertThat(command.trainerLicense()).isNull();
        assertThat(command.refereeLicense()).isNull();
        assertThat(command.drivingLicenseGroup()).isNull();
        assertThat(command.dietaryRestrictions()).isNull();
        assertThat(command.birthNumber()).isNull();
        assertThat(command.bankAccountNumber()).isNull();
        assertThat(command.firstName()).isEqualTo(baseline.firstName());
        assertThat(command.lastName()).isEqualTo(baseline.lastName());
        assertThat(command.dateOfBirth()).isEqualTo(baseline.dateOfBirth());
        assertThat(command.gender()).isEqualTo(baseline.gender());
        assertThat(command.nationality()).isEqualTo(baseline.nationality());
    }
}
