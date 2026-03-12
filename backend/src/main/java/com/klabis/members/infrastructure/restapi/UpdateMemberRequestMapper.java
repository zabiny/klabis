package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.application.InvalidUpdateException;
import com.klabis.members.domain.*;

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
            RefereeLicense refereeLicense = toRefereeLicense(request.refereeLicense());
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
                    medicalCourse, trainerLicense, refereeLicense, dietaryRestrictions, guardian,
                    firstName, lastName, dateOfBirth, gender, birthNumber, updatedBy
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    static Member.SelfUpdate toSelfUpdateCommand(UpdateMemberRequest request) {
        try {
            return new Member.SelfUpdate(
                    toEmailAddress(request.email()),
                    toPhoneNumber(request.phone()),
                    toAddress(request.address()),
                    toString(request.chipNumber()),
                    toString(request.nationality()),
                    toBankAccountNumber(request.bankAccountNumber()),
                    toIdentityCard(request.identityCard()),
                    toEnum(request.drivingLicenseGroup()),
                    toMedicalCourse(request.medicalCourse()),
                    toTrainerLicense(request.trainerLicense()),
                    toRefereeLicense(request.refereeLicense()),
                    toString(request.dietaryRestrictions()),
                    toGuardianInformation(request.guardian())
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }

    static Member.SelfUpdate toSelfUpdateCommand(SelfUpdateMemberRequest request) {
        try {
            return new Member.SelfUpdate(
                    toEmailAddress(request.email()),
                    toPhoneNumber(request.phone()),
                    toAddress(request.address()),
                    toString(request.chipNumber()),
                    toString(request.nationality()),
                    toBankAccountNumber(request.bankAccountNumber()),
                    toIdentityCard(request.identityCard()),
                    toEnum(request.drivingLicenseGroup()),
                    toMedicalCourse(request.medicalCourse()),
                    toTrainerLicense(request.trainerLicense()),
                    toRefereeLicense(request.refereeLicense()),
                    toString(request.dietaryRestrictions()),
                    toGuardianInformation(request.guardian())
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
        if (!address.isProvided()) {
            return null;
        }
        AddressRequest a = address.throwIfNotProvided();
        return Address.of(a.street(), a.city(), a.postalCode(), a.country());
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
        if (!identityCard.isProvided()) {
            return null;
        }
        IdentityCardDto dto = identityCard.throwIfNotProvided();
        return IdentityCard.of(dto.cardNumber(), dto.validityDate());
    }

    private static <T> T toEnum(com.klabis.common.patch.PatchField<T> enumField) {
        return enumField.isProvided() ? enumField.throwIfNotProvided() : null;
    }

    private static MedicalCourse toMedicalCourse(com.klabis.common.patch.PatchField<MedicalCourseDto> medicalCourse) {
        if (!medicalCourse.isProvided()) {
            return null;
        }
        MedicalCourseDto dto = medicalCourse.throwIfNotProvided();
        return MedicalCourse.of(dto.completionDate(), dto.validityDate());
    }

    private static TrainerLicense toTrainerLicense(com.klabis.common.patch.PatchField<TrainerLicenseDto> trainerLicense) {
        if (!trainerLicense.isProvided()) {
            return null;
        }
        TrainerLicenseDto dto = trainerLicense.throwIfNotProvided();
        return TrainerLicense.of(dto.level(), dto.validityDate());
    }

    private static RefereeLicense toRefereeLicense(com.klabis.common.patch.PatchField<RefereeLicenseDto> refereeLicense) {
        if (!refereeLicense.isProvided()) {
            return null;
        }
        RefereeLicenseDto dto = refereeLicense.throwIfNotProvided();
        return RefereeLicense.of(dto.level(), dto.validityDate());
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
        if (!guardian.isProvided()) {
            return null;
        }
        GuardianDTO dto = guardian.throwIfNotProvided();
        return new GuardianInformation(
                dto.firstName(),
                dto.lastName(),
                dto.relationship(),
                dto.email() != null ? EmailAddress.of(dto.email()) : null,
                dto.phone() != null ? PhoneNumber.of(dto.phone()) : null);
    }
}
