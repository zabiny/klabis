package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.application.InvalidUpdateException;
import com.klabis.members.domain.*;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.Optional;
import java.util.function.Function;

class UpdateMemberRequestMapper {

    private UpdateMemberRequestMapper() {}

    static Member.UpdateMember toCommand(UpdateMemberRequest request, UserId updatedBy) {
        try {
            return new Member.UpdateMember(
                    map(request.email(), EmailAddress::of),
                    map(request.phone(), PhoneNumber::of),
                    map(request.address(), UpdateMemberRequestMapper::toAddress),
                    request.chipNumber(),
                    unwrap(request.nationality()),
                    map(request.bankAccountNumber(), UpdateMemberRequestMapper::toBankAccountNumber),
                    map(request.identityCard(), dto -> IdentityCard.of(dto.cardNumber(), dto.validityDate())),
                    request.drivingLicenseGroup(),
                    map(request.medicalCourse(), UpdateMemberRequestMapper::toMedicalCourse),
                    map(request.trainerLicense(), dto -> TrainerLicense.of(dto.level(), dto.validityDate())),
                    map(request.refereeLicense(), dto -> RefereeLicense.of(dto.level(), dto.validityDate())),
                    request.dietaryRestrictions(),
                    map(request.guardian(), UpdateMemberRequestMapper::toGuardianInformation),
                    unwrap(request.firstName()),
                    unwrap(request.lastName()),
                    unwrap(request.dateOfBirth()),
                    request.gender(),
                    map(request.birthNumber(), UpdateMemberRequestMapper::toBirthNumber),
                    updatedBy
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    /**
     * Applies {@code converter} only to a present non-null value, so an explicit null survives as a
     * present null — the domain reads that as "clear this field" — instead of being handed to a
     * factory that would reject it.
     */
    private static <T, R> JsonNullable<R> map(JsonNullable<T> field, Function<T, R> converter) {
        return field.map(value -> value == null ? null : converter.apply(value));
    }

    /**
     * For the fields of {@link PersonalInformation}, which have no cleared state: an explicit null
     * collapses to "retain", matching the plain-null contract of the command's required fields.
     * gender needs no unwrapping — it is not wrapped, see PatchRequestWrapperArchitectureTest.
     */
    private static <T> T unwrap(JsonNullable<T> field) {
        return field.isPresent() ? field.orElseThrow() : null;
    }

    private static Address toAddress(AddressRequest a) {
        return Address.of(a.street(), a.city(), a.postalCode(), a.country());
    }

    private static BankAccountNumber toBankAccountNumber(String value) {
        return value.isBlank() ? null : BankAccountNumber.of(value);
    }

    private static MedicalCourse toMedicalCourse(MedicalCourseDto dto) {
        return MedicalCourse.of(dto.completionDate(), Optional.ofNullable(dto.validityDate()));
    }

    private static BirthNumber toBirthNumber(String value) {
        return value.isBlank() ? null : BirthNumber.of(value);
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
