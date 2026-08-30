package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.application.InvalidUpdateException;
import com.klabis.members.domain.*;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Applies a PATCH {@link UpdateMemberRequest} onto a fully pre-filled {@link Member.UpdateMember}
 * baseline (from {@link com.klabis.members.application.ManagementPort#prefilledUpdateCommand}).
 * <p>
 * This is where the three PATCH states are resolved, so the domain never sees {@link JsonNullable}:
 * an <em>undefined</em> field leaves the baseline value in place, a <em>present-null</em> field
 * clears it, and a <em>present</em> value sets it. Fields of {@link PersonalInformation} have no
 * cleared state, so a present-null there also just retains the baseline.
 */
class UpdateMemberRequestMapper {

    private UpdateMemberRequestMapper() {}

    static Member.UpdateMember toCommand(UpdateMemberRequest request, Member.UpdateMember prefilled, UserId updatedBy) {
        try {
            var b = MemberUpdateMemberBuilder.builder(prefilled).updatedBy(updatedBy);

            overlay(request.email(), v -> b.email(v == null ? null : EmailAddress.of(v)));
            overlay(request.phone(), v -> b.phone(v == null ? null : PhoneNumber.of(v)));
            overlay(request.address(), v -> b.address(v == null ? null : toAddress(v)));
            overlay(request.chipNumber(), b::chipNumber);
            overlay(request.bankAccountNumber(), v -> b.bankAccountNumber(toBankAccountNumber(v)));
            overlay(request.identityCard(), v -> b.identityCard(v == null ? null : IdentityCard.of(v.cardNumber(), v.validityDate())));
            overlay(request.drivingLicenseGroup(), v -> b.drivingLicenseGroup(toDrivingLicenseGroup(v)));
            overlay(request.medicalCourse(), v -> b.medicalCourse(v == null ? null : toMedicalCourse(v)));
            overlay(request.trainerLicense(), v -> b.trainerLicense(v == null ? null : TrainerLicense.of(toTrainerLevel(v.level()), v.validityDate())));
            overlay(request.refereeLicense(), v -> b.refereeLicense(v == null ? null : RefereeLicense.of(toRefereeLevel(v.level()), v.validityDate())));
            overlay(request.dietaryRestrictions(), b::dietaryRestrictions);
            overlay(request.guardian(), v -> b.guardian(v == null ? null : toGuardianInformation(v)));
            overlay(request.birthNumber(), v -> b.birthNumber(toBirthNumber(v)));

            overlayValue(request.nationality(), b::nationality);
            overlayValue(request.firstName(), b::firstName);
            overlayValue(request.lastName(), b::lastName);
            overlayValue(request.dateOfBirth(), b::dateOfBirth);
            overlayValue(request.gender(), v -> b.gender(toGender(v)));

            return b.build();
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    /**
     * Runs {@code apply} for a present field (including a present null, which means "clear"); an
     * undefined field is left alone so the pre-filled baseline value stands.
     */
    private static <T> void overlay(JsonNullable<T> field, Consumer<T> apply) {
        if (field.isPresent()) {
            apply.accept(field.get());
        }
    }

    /**
     * For {@link PersonalInformation} fields, which have no cleared state: only a present non-null
     * value overrides the baseline; a present null retains it, matching the pre-refactor contract.
     */
    private static <T> void overlayValue(JsonNullable<T> field, Consumer<T> apply) {
        field.ifPresent(value -> {
            if (value != null) {
                apply.accept(value);
            }
        });
    }

    private static Address toAddress(AddressRequest a) {
        return Address.of(a.street(), a.city(), a.postalCode(), a.country());
    }

    private static BankAccountNumber toBankAccountNumber(String value) {
        return value == null || value.isBlank() ? null : BankAccountNumber.of(value);
    }

    private static MedicalCourse toMedicalCourse(MedicalCourseDto dto) {
        return MedicalCourse.of(dto.completionDate(), Optional.ofNullable(dto.validityDate()));
    }

    private static BirthNumber toBirthNumber(String value) {
        return value == null || value.isBlank() ? null : BirthNumber.of(value);
    }

    private static com.klabis.members.domain.DrivingLicenseGroup toDrivingLicenseGroup(DrivingLicenseGroup dto) {
        return dto == null ? null : com.klabis.members.domain.DrivingLicenseGroup.valueOf(dto.name());
    }

    private static com.klabis.members.domain.Gender toGender(UpdateMemberRequestGender dto) {
        return dto == null ? null : com.klabis.members.domain.Gender.valueOf(dto.name());
    }

    private static com.klabis.members.domain.TrainerLevel toTrainerLevel(TrainerLicenseDtoLevel dto) {
        return dto == null ? null : com.klabis.members.domain.TrainerLevel.valueOf(dto.name());
    }

    private static com.klabis.members.domain.RefereeLevel toRefereeLevel(RefereeLicenseDtoLevel dto) {
        return dto == null ? null : com.klabis.members.domain.RefereeLevel.valueOf(dto.name());
    }

    private static GuardianInformation toGuardianInformation(GuardianDTO dto) {
        return new GuardianInformation(
                dto.firstName(),
                dto.lastName(),
                dto.relationship(),
                dto.email() != null ? EmailAddress.of(dto.email()) : null,
                dto.phone() != null ? PhoneNumber.of(dto.phone()) : null);
    }
}
