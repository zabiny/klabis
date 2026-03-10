package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.application.InvalidUpdateException;
import com.klabis.members.domain.*;

/**
 * Converts {@link UpdateMemberRequest} REST DTO to domain commands.
 * Conversion lives in the infrastructure layer; domain is kept free of REST types.
 */
class UpdateMemberRequestMapper {

    private UpdateMemberRequestMapper() {}

    static Member.UpdateMemberByAdmin toAdminCommand(UpdateMemberRequest request, UserId updatedBy) {
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
            String dietaryRestrictions = toString(request.dietaryRestrictions());
            GuardianInformation guardian = toGuardianInformation(request.guardian());
            String firstName = toString(request.firstName());
            String lastName = toString(request.lastName());
            java.time.LocalDate dateOfBirth = toLocalDate(request.dateOfBirth());
            Gender gender = toEnum(request.gender());
            BirthNumber birthNumber = toBirthNumber(request.birthNumber());

            return new Member.UpdateMemberByAdmin(
                    email, phone, address, chipNumber, nationality,
                    bankAccountNumber, identityCard, drivingLicenseGroup,
                    medicalCourse, trainerLicense, dietaryRestrictions, guardian,
                    firstName, lastName, dateOfBirth, gender, birthNumber, updatedBy
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    static Member.SelfUpdate toSelfUpdateCommand(UpdateMemberRequest request) {
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
            String dietaryRestrictions = toString(request.dietaryRestrictions());
            GuardianInformation guardian = toGuardianInformation(request.guardian());

            return new Member.SelfUpdate(
                    email, phone, address, chipNumber, nationality,
                    bankAccountNumber, identityCard, drivingLicenseGroup,
                    medicalCourse, trainerLicense, dietaryRestrictions, guardian
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    private static EmailAddress toEmailAddress(com.klabis.common.patch.PatchField<String> email) {
        return email.isProvided() ? EmailAddress.of(email.throwIfNotProvided()) : null;
    }

    private static PhoneNumber toPhoneNumber(com.klabis.common.patch.PatchField<String> phone) {
        return phone.isProvided() ? PhoneNumber.of(phone.throwIfNotProvided()) : null;
    }

    private static Address toAddress(com.klabis.common.patch.PatchField<AddressRequest> address) {
        return address.isProvided()
            ? Address.of(address.throwIfNotProvided().street(), address.throwIfNotProvided().city(), address.throwIfNotProvided().postalCode(), address.throwIfNotProvided().country())
            : null;
    }

    private static String toString(com.klabis.common.patch.PatchField<String> value) {
        return value.isProvided() ? value.throwIfNotProvided() : null;
    }

    private static BankAccountNumber toBankAccountNumber(com.klabis.common.patch.PatchField<String> bankAccountNumber) {
        if (bankAccountNumber.isProvided()) {
            String value = bankAccountNumber.throwIfNotProvided();
            if (!value.isBlank()) {
                return BankAccountNumber.of(value);
            }
        }
        return null;
    }

    private static IdentityCard toIdentityCard(com.klabis.common.patch.PatchField<IdentityCardDto> identityCard) {
        return identityCard.isProvided()
            ? IdentityCard.of(identityCard.throwIfNotProvided().cardNumber(), identityCard.throwIfNotProvided().validityDate())
            : null;
    }

    private static <T> T toEnum(com.klabis.common.patch.PatchField<T> enumField) {
        return enumField.isProvided() ? enumField.throwIfNotProvided() : null;
    }

    private static MedicalCourse toMedicalCourse(com.klabis.common.patch.PatchField<MedicalCourseDto> medicalCourse) {
        return medicalCourse.isProvided()
            ? MedicalCourse.of(medicalCourse.throwIfNotProvided().completionDate(), medicalCourse.throwIfNotProvided().validityDate())
            : null;
    }

    private static TrainerLicense toTrainerLicense(com.klabis.common.patch.PatchField<TrainerLicenseDto> trainerLicense) {
        return trainerLicense.isProvided()
            ? TrainerLicense.of(trainerLicense.throwIfNotProvided().licenseNumber(), trainerLicense.throwIfNotProvided().validityDate())
            : null;
    }

    private static java.time.LocalDate toLocalDate(com.klabis.common.patch.PatchField<java.time.LocalDate> dateField) {
        return dateField.isProvided() ? dateField.throwIfNotProvided() : null;
    }

    private static BirthNumber toBirthNumber(com.klabis.common.patch.PatchField<String> birthNumber) {
        if (birthNumber.isProvided()) {
            String value = birthNumber.throwIfNotProvided();
            if (!value.isBlank()) {
                return BirthNumber.of(value);
            }
        }
        return null;
    }

    private static GuardianInformation toGuardianInformation(com.klabis.common.patch.PatchField<GuardianDTO> guardian) {
        return guardian.isProvided()
            ? new GuardianInformation(
                guardian.throwIfNotProvided().firstName(),
                guardian.throwIfNotProvided().lastName(),
                guardian.throwIfNotProvided().relationship(),
                guardian.throwIfNotProvided().email() != null ? EmailAddress.of(guardian.throwIfNotProvided().email()) : null,
                guardian.throwIfNotProvided().phone() != null ? PhoneNumber.of(guardian.throwIfNotProvided().phone()) : null)
            : null;
    }
}
