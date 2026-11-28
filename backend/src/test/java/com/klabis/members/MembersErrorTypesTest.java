package com.klabis.members;

import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.users.UserId;
import com.klabis.members.application.AccountActivationNotAvailableException;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.application.NotSoleGuardianGroupException;
import com.klabis.members.application.SuspensionBlockedException;
import com.klabis.members.domain.BirthNumberNotAllowedException;
import com.klabis.members.domain.DateOfBirthInFutureException;
import com.klabis.members.domain.GuardiansNotAllowedException;
import com.klabis.members.domain.MedicalCourse;
import com.klabis.members.domain.MedicalCourseValidityException;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberAlreadyActiveException;
import com.klabis.members.domain.MemberAlreadySuspendedException;
import com.klabis.members.domain.MemberDataRequiredException;
import com.klabis.members.domain.MissingDataItem;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.members.domain.RegistrationNumberAlreadyInUseException;
import com.klabis.members.legalguardian.application.LegalGuardianNotFoundException;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupWithoutGuardianException;
import com.klabis.members.legalguardiangroup.domain.OnlyMinorsAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Members error types")
class MembersErrorTypesTest {

    private static final String REGISTER_REQUEST = "com.klabis.members.infrastructure.restapi.RegisterMemberRequest";
    private static final String UPDATE_REQUEST = "com.klabis.members.infrastructure.restapi.UpdateMemberRequest";

    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());

    private static void assertError(UserFacingError error, String type, Map<String, Object> parameters) {
        assertThat(error.errorType()).isEqualTo(type);
        assertThat(error.messageParameters()).containsExactlyInAnyOrderEntriesOf(parameters);
        assertThat(error.field()).isNull();
        assertThat(error.messageParameters().values().toString()).doesNotContain(MEMBER_ID.uuid().toString());
    }

    @Test
    @DisplayName("not found errors carry no identifier")
    void notFoundErrors() {
        assertError(new MemberNotFoundException(MEMBER_ID), "member-not-found", Map.of());
        assertError(new LegalGuardianNotFoundException(new UserId(MEMBER_ID.uuid())), "legal-guardian-not-found",
                Map.of());
    }

    @Test
    @DisplayName("legal guardian rules")
    void legalGuardianRules() {
        assertError(new LegalGuardianEmailAlreadyInUseException("a@b.cz"), "legal-guardian-email-already-in-use",
                Map.of("email", "a@b.cz"));
        assertError(new GuardianNotFoundException(new UserId(MEMBER_ID.uuid())), "guardian-not-found", Map.of());
        assertError(new LegalGuardianGroupWithoutGuardianException(), "legal-guardian-group-without-guardian", Map.of());
        assertError(new OnlyMinorsAllowedException(MEMBER_ID), "only-minors-allowed", Map.of());
        assertError(new NotSoleGuardianGroupException(new LegalGuardianGroupId(UUID.randomUUID())),
                "not-sole-guardian-group", Map.of());
    }

    @Test
    @DisplayName("account activation")
    void accountActivation() {
        assertError(new AccountActivationNotAvailableException(MEMBER_ID), "account-activation-not-available", Map.of());
    }

    @Test
    @DisplayName("suspension blocked")
    void suspensionBlocked() {
        SuspensionBlockedException ex = new SuspensionBlockedException(List.of(), null, List.of());

        assertThat(ex.errorType()).isEqualTo("suspension-blocked");
    }

    @ParameterizedTest(name = "{0} is reported as {1} bound to field {2}")
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
    @DisplayName("member state conflicts have their own types")
    void stateConflicts() {
        assertThat(new MemberAlreadySuspendedException().errorType()).isEqualTo("member-already-suspended");
        assertThat(new MemberAlreadyActiveException().errorType()).isEqualTo("member-already-active");
    }

    @Test
    @DisplayName("a birth number for a non-Czech national is refused at the birth number field")
    void birthNumberNotAllowed() {
        Member.RegisterMember command = aMember().withNationality("SK").withBirthNumber("900515/1234")
                .toRegisterMemberCommand();

        assertThatThrownBy(() -> Member.importFromOris(command))
                .isInstanceOfSatisfying(BirthNumberNotAllowedException.class, ex -> {
                    assertThat(ex.errorType()).isEqualTo("birth-number-not-allowed");
                    assertThat(ex.field()).isEqualTo("birthNumber");
                });
    }

    @Test
    @DisplayName("a date of birth in the future is reported at the date of birth field")
    void dateOfBirthInFuture() {
        DateOfBirthInFutureException ex = new DateOfBirthInFutureException();

        assertThat(ex.errorType()).isEqualTo("date-of-birth-in-future");
        assertThat(ex.field()).isEqualTo("dateOfBirth");
        assertThat(ex.messageParameters()).isEmpty();
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
        assertThat(ex.messageParameters()).containsExactly(Map.entry("registrationNumber", "ZBM9001"));
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
