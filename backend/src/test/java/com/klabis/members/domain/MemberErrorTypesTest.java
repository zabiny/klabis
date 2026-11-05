package com.klabis.members.domain;

import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Members domain error types")
class MemberErrorTypesTest {

    private static final String REGISTER_REQUEST = "com.klabis.members.infrastructure.restapi.RegisterMemberRequest";
    private static final String UPDATE_REQUEST = "com.klabis.members.infrastructure.restapi.UpdateMemberRequest";

    @ParameterizedTest
    @CsvSource({
            "EMAIL, email-required, email",
            "PHONE, phone-required, phone",
            "ADDRESS, address-required, address",
            "BIRTH_NUMBER, birth-number-required, birthNumber",
            "GUARDIAN, guardian-required,"
    })
    @DisplayName("a missing detail is reported under its own type, bound to its form field")
    void missingDataTypes(MissingDataItem item, String type, String field) {
        MemberDataRequiredException ex = new MemberDataRequiredException(item);

        assertThat(ex.errorType()).isEqualTo(type);
        assertThat(ex.field()).isEqualTo(field);
        assertThat(ex.messageParameters()).isEmpty();
        assertThat(ex.errorTypes()).contains(type);
    }

    @Test
    @DisplayName("registering without any contact fails with email-required")
    void registerWithoutContact() {
        assertThatThrownBy(() -> Member.register(MemberRegisterMemberBuilder.builder()
                .id(new MemberId(UUID.randomUUID()))
                .registrationNumber(new RegistrationNumber("ZBM9001"))
                .personalInformation(PersonalInformation.of("Jan", "Novák", LocalDate.of(1990, 5, 15), "CZ", Gender.MALE))
                .address(new Address("Ulice 1", "Město", "11000", "CZ"))
                .build()))
                .isInstanceOf(MemberDataRequiredException.class)
                .extracting(e -> ((UserFacingError) e).errorType())
                .isEqualTo("email-required");
    }

    @Test
    @DisplayName("suspending a suspended member and resuming an active one have their own types")
    void suspendAndResume() {
        Member member = aMember().build();
        Member.SuspendMembership suspend = MemberSuspendMembershipBuilder.builder()
                .suspendedBy(new UserId(UUID.randomUUID())).reason(DeactivationReason.ODHLASKA).build();
        assertThatThrownBy(() -> {
            member.suspend(suspend);
            member.suspend(suspend);
        }).isInstanceOf(MemberAlreadySuspendedException.class);
        assertThat(new MemberAlreadySuspendedException().errorType()).isEqualTo("member-already-suspended");

        Member active = aMember().build();
        assertThatThrownBy(() -> active.resume(MemberResumeMembershipBuilder.builder()
                .resumedBy(new UserId(UUID.randomUUID())).build()))
                .isInstanceOf(MemberAlreadyActiveException.class);
        assertThat(new MemberAlreadyActiveException().errorType()).isEqualTo("member-already-active");
    }

    @Test
    @DisplayName("a birth number for a non-Czech national is refused at the birth number field")
    void birthNumberNotAllowed() {
        Member.RegisterMember command = aMember().withNationality("SK").withBirthNumber("900515/1234")
                .toRegisterMemberCommand();

        assertThatThrownBy(() -> Member.importFromOris(command))
                .isInstanceOf(BirthNumberNotAllowedException.class);
        BirthNumberNotAllowedException ex = new BirthNumberNotAllowedException();
        assertThat(ex.errorType()).isEqualTo("birth-number-not-allowed");
        assertThat(ex.field()).isEqualTo("birthNumber");
    }

    @Test
    @DisplayName("a date of birth in the future is refused at the date of birth field")
    void dateOfBirthInFuture() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> PersonalInformation.of("Jan", "Novák", tomorrow, "CZ", Gender.MALE))
                .isInstanceOfSatisfying(DateOfBirthInFutureException.class, ex -> {
                    assertThat(ex.errorType()).isEqualTo("date-of-birth-in-future");
                    assertThat(ex.field()).isEqualTo("dateOfBirth");
                    assertThat(ex.messageParameters()).isEmpty();
                });
    }

    @Test
    @DisplayName("a medical course valid until before its completion is refused at the medical course field")
    void medicalCourseValidity() {
        LocalDate completion = LocalDate.of(2024, 5, 1);

        assertThatThrownBy(() -> MedicalCourse.of(completion, Optional.of(completion)))
                .isInstanceOfSatisfying(MedicalCourseValidityException.class, ex -> {
                    assertThat(ex.errorType()).isEqualTo("medical-course-validity-not-after-completion");
                    assertThat(ex.field()).isEqualTo("medicalCourse");
                });
    }

    @Test
    @DisplayName("guardian rules have one type per way of breaking them, each bound to its field")
    void guardiansNotAllowed() {
        GuardiansNotAllowedException adult = GuardiansNotAllowedException.adultWithGuardians();
        GuardiansNotAllowedException takeOver = GuardiansNotAllowedException.takeOverByMinor();

        assertThat(adult.errorType()).isEqualTo("adult-member-cannot-have-guardians");
        assertThat(adult.field()).isEqualTo("legalGuardians");
        assertThat(takeOver.errorType()).isEqualTo("minor-cannot-take-over-guardian");
        assertThat(takeOver.field()).isEqualTo("legalGuardianUserId");
        assertThat(adult.errorTypes()).containsExactlyInAnyOrder(
                "adult-member-cannot-have-guardians", "minor-cannot-take-over-guardian");
    }

    @Test
    @DisplayName("registration number conflict names the number, not an identifier")
    void registrationNumberInUse() {
        RegistrationNumberAlreadyInUseException ex =
                new RegistrationNumberAlreadyInUseException(new RegistrationNumber("ZBM9001"), null);

        assertThat(ex.errorType()).isEqualTo("registration-number-already-in-use");
        assertThat(ex.messageParameters()).containsExactly(java.util.Map.entry("registrationNumber", "ZBM9001"));
    }

    @Test
    @DisplayName("bound fields exist as properties of the registration and update requests")
    void boundFieldsAreRequestProperties() throws Exception {
        Set<String> registerFields = recordComponents(REGISTER_REQUEST);
        Set<String> updateFields = recordComponents(UPDATE_REQUEST);

        assertThat(registerFields).contains("email", "phone", "address", "birthNumber", "dateOfBirth",
                "legalGuardians", "legalGuardianUserId", "bankAccountNumber");
        assertThat(updateFields).contains("email", "phone", "address", "birthNumber", "medicalCourse",
                "bankAccountNumber");
    }

    private static Set<String> recordComponents(String className) throws ClassNotFoundException {
        return Set.copyOf(Arrays.stream(Class.forName(className).getRecordComponents())
                .map(RecordComponent::getName).toList());
    }
}
