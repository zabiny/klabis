package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.application.InvalidUpdateException;
import com.klabis.members.domain.*;
import org.openapitools.jackson.nullable.JsonNullable;

class UpdateMemberRequestMapper {

    private UpdateMemberRequestMapper() {}

    static Member.UpdateMember toCommand(UpdateMemberRequest request, UserId updatedBy) {
        try {
            EmailAddress email = toEmailAddress(request.email());
            PhoneNumber phone = toPhoneNumber(request.phone());
            Address address = toAddress(request.address());
            String chipNumber = toString(request.chipNumber());
            String nationality = toString(request.nationality());
            BankAccountNumber bankAccountNumber = toBankAccountNumber(request.bankAccountNumber());
            IdentityCard identityCard = toIdentityCard(request.identityCard());
            DrivingLicenseGroup drivingLicenseGroup = toEnum(request.drivingLicenseGroup());
            MedicalCourse medicalCourse = toMedicalCourse(request.medicalCourse());
            TrainerLicense trainerLicense = toTrainerLicense(request.trainerLicense());
            RefereeLicense refereeLicense = toRefereeLicense(request.refereeLicense());
            String dietaryRestrictions = toString(request.dietaryRestrictions());
            GuardianInformation guardian = toGuardianInformation(request.guardian());
            String firstName = toString(request.firstName());
            String lastName = toString(request.lastName());
            java.time.LocalDate dateOfBirth = toLocalDate(request.dateOfBirth());
            Gender gender = toEnum(request.gender());
            BirthNumber birthNumber = toBirthNumber(request.birthNumber());

            return new Member.UpdateMember(
                    email, phone, address, chipNumber, nationality,
                    bankAccountNumber, identityCard, drivingLicenseGroup,
                    medicalCourse, trainerLicense, refereeLicense, dietaryRestrictions, guardian,
                    firstName, lastName, dateOfBirth, gender, birthNumber, updatedBy
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    private static EmailAddress toEmailAddress(JsonNullable<String> email) {
        return email.isPresent() ? EmailAddress.of(email.orElseThrow()) : null;
    }

    private static PhoneNumber toPhoneNumber(JsonNullable<String> phone) {
        return phone.isPresent() ? PhoneNumber.of(phone.orElseThrow()) : null;
    }

    private static Address toAddress(JsonNullable<AddressRequest> address) {
        if (!address.isPresent()) {
            return null;
        }
        AddressRequest a = address.orElseThrow();
        if (a == null) return null;
        return Address.of(a.street(), a.city(), a.postalCode(), a.country());
    }

    private static String toString(JsonNullable<String> value) {
        return value.isPresent() ? value.orElseThrow() : null;
    }

    private static BankAccountNumber toBankAccountNumber(JsonNullable<String> bankAccountNumber) {
        if (bankAccountNumber.isPresent()) {
            String value = bankAccountNumber.orElseThrow();
            if (value != null && !value.isBlank()) {
                return BankAccountNumber.of(value);
            }
        }
        return null;
    }

    private static IdentityCard toIdentityCard(JsonNullable<IdentityCardDto> identityCard) {
        if (!identityCard.isPresent()) {
            return null;
        }
        IdentityCardDto dto = identityCard.orElseThrow();
        if (dto == null) return null;
        return IdentityCard.of(dto.cardNumber(), dto.validityDate());
    }

    private static <T> T toEnum(JsonNullable<T> enumField) {
        return enumField.isPresent() ? enumField.orElseThrow() : null;
    }

    private static MedicalCourse toMedicalCourse(JsonNullable<MedicalCourseDto> medicalCourse) {
        if (!medicalCourse.isPresent()) {
            return null;
        }
        MedicalCourseDto dto = medicalCourse.orElseThrow();
        if (dto == null) return null;
        return MedicalCourse.of(dto.completionDate(), java.util.Optional.ofNullable(dto.validityDate()));
    }

    private static TrainerLicense toTrainerLicense(JsonNullable<TrainerLicenseDto> trainerLicense) {
        if (!trainerLicense.isPresent()) {
            return null;
        }
        TrainerLicenseDto dto = trainerLicense.orElseThrow();
        if (dto == null) return null;
        return TrainerLicense.of(dto.level(), dto.validityDate());
    }

    private static RefereeLicense toRefereeLicense(JsonNullable<RefereeLicenseDto> refereeLicense) {
        if (!refereeLicense.isPresent()) {
            return null;
        }
        RefereeLicenseDto dto = refereeLicense.orElseThrow();
        if (dto == null) return null;
        return RefereeLicense.of(dto.level(), dto.validityDate());
    }

    private static java.time.LocalDate toLocalDate(JsonNullable<java.time.LocalDate> dateField) {
        return dateField.isPresent() ? dateField.orElseThrow() : null;
    }

    private static BirthNumber toBirthNumber(JsonNullable<String> birthNumber) {
        if (birthNumber.isPresent()) {
            String value = birthNumber.orElseThrow();
            if (value != null && !value.isBlank()) {
                return BirthNumber.of(value);
            }
        }
        return null;
    }

    private static GuardianInformation toGuardianInformation(JsonNullable<GuardianDTO> guardian) {
        if (!guardian.isPresent()) {
            return null;
        }
        GuardianDTO dto = guardian.orElseThrow();
        if (dto == null) return null;
        return new GuardianInformation(
                dto.firstName(),
                dto.lastName(),
                dto.relationship(),
                dto.email() != null ? EmailAddress.of(dto.email()) : null,
                dto.phone() != null ? PhoneNumber.of(dto.phone()) : null);
    }
}
