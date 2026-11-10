package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.*;
import com.klabis.members.infrastructure.restapi.InvalidFieldValueException.Kind;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.Optional;

import static com.klabis.common.patch.PatchOverlay.overlay;
import static com.klabis.common.patch.PatchOverlay.overlayNonNull;

/**
 * Applies a PATCH {@link UpdateMemberRequest} onto a fully pre-filled {@link Member.UpdateMember}
 * baseline (from {@link com.klabis.members.application.ManagementPort#prefilledUpdateCommand}).
 * <p>
 * This is where the three PATCH states are resolved, so the domain never sees {@link JsonNullable}:
 * an <em>undefined</em> field leaves the baseline value in place, a <em>present-null</em> field
 * clears it, and a <em>present</em> value sets it, as defined by {@link com.klabis.common.patch.PatchOverlay}.
 * Fields of {@link PersonalInformation} have no cleared state, so a present-null there retains the baseline.
 */
class UpdateMemberRequestMapper {

    private UpdateMemberRequestMapper() {}

    static Member.UpdateMember toCommand(UpdateMemberRequest request, Member.UpdateMember prefilled, UserId updatedBy) {
        var b = MemberUpdateMemberBuilder.builder(prefilled).updatedBy(updatedBy);

        overlay(request.email(), v -> b.email(v == null ? null : InvalidFieldValueException.parse(Kind.EMAIL, () -> EmailAddress.of(v))));
        overlay(request.phone(), v -> b.phone(v == null ? null : InvalidFieldValueException.parse(Kind.PHONE, () -> PhoneNumber.of(v))));
        overlay(request.address(), v -> b.address(v == null ? null : toAddress(v)));
        overlay(request.chipNumber(), b::chipNumber);
        overlay(request.bankAccountNumber(), v -> b.bankAccountNumber(toBankAccountNumber(v)));
        overlay(request.identityCard(), v -> b.identityCard(v == null ? null : IdentityCard.of(v.cardNumber(), v.validityDate())));
        overlay(request.drivingLicenseGroup(), v -> b.drivingLicenseGroup(toDrivingLicenseGroup(v)));
        overlay(request.medicalCourse(), v -> b.medicalCourse(v == null ? null : toMedicalCourse(v)));
        overlay(request.trainerLicense(), v -> b.trainerLicense(v == null ? null : TrainerLicense.of(toTrainerLevel(v.level()), v.validityDate())));
        overlay(request.refereeLicense(), v -> b.refereeLicense(v == null ? null : RefereeLicense.of(toRefereeLevel(v.level()), v.validityDate())));
        overlay(request.dietaryRestrictions(), b::dietaryRestrictions);
        overlay(request.birthNumber(), v -> b.birthNumber(toBirthNumber(v)));

        overlayNonNull(request.nationality(), b::nationality);
        overlayNonNull(request.firstName(), b::firstName);
        overlayNonNull(request.lastName(), b::lastName);
        overlayNonNull(request.dateOfBirth(), b::dateOfBirth);
        overlayNonNull(request.gender(), v -> b.gender(toGender(v)));

        return b.build();
    }



    private static Address toAddress(AddressRequest a) {
        return Address.of(a.street(), a.city(), a.postalCode(), a.country());
    }

    private static BankAccountNumber toBankAccountNumber(String value) {
        return value == null || value.isBlank() ? null
                : InvalidFieldValueException.parse(Kind.BANK_ACCOUNT_NUMBER, () -> BankAccountNumber.of(value));
    }

    private static MedicalCourse toMedicalCourse(MedicalCourseDto dto) {
        return MedicalCourse.of(dto.completionDate(), Optional.ofNullable(dto.validityDate()));
    }

    private static BirthNumber toBirthNumber(String value) {
        return value == null || value.isBlank() ? null
                : InvalidFieldValueException.parse(Kind.BIRTH_NUMBER, () -> BirthNumber.of(value));
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
}
