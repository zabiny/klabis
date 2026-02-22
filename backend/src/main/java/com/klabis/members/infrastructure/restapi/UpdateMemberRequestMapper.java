package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.*;
import com.klabis.members.management.InvalidUpdateException;

/**
 * Converts {@link UpdateMemberRequest} REST DTO to domain commands.
 * Conversion lives in the infrastructure layer; domain is kept free of REST types.
 */
class UpdateMemberRequestMapper {

    private UpdateMemberRequestMapper() {}

    static Member.UpdateMemberByAdmin toAdminCommand(UpdateMemberRequest request) {
        try {
            EmailAddress email = request.email()
                    .map(EmailAddress::of)
                    .orElse(null);
            PhoneNumber phone = request.phone()
                    .map(PhoneNumber::of)
                    .orElse(null);
            Address address = request.address()
                    .map(a -> Address.of(a.street(), a.city(), a.postalCode(), a.country()))
                    .orElse(null);
            String chipNumber = request.chipNumber().orElse(null);
            BankAccountNumber bankAccountNumber = request.bankAccountNumber()
                    .filter(s -> !s.isBlank())
                    .map(BankAccountNumber::of)
                    .orElse(null);
            IdentityCard identityCard = request.identityCard()
                    .map(dto -> IdentityCard.of(dto.cardNumber(), dto.validityDate()))
                    .orElse(null);
            DrivingLicenseGroup drivingLicenseGroup = request.drivingLicenseGroup().orElse(null);
            MedicalCourse medicalCourse = request.medicalCourse()
                    .map(dto -> MedicalCourse.of(dto.completionDate(), dto.validityDate()))
                    .orElse(null);
            TrainerLicense trainerLicense = request.trainerLicense()
                    .map(dto -> TrainerLicense.of(dto.licenseNumber(), dto.validityDate()))
                    .orElse(null);
            String dietaryRestrictions = request.dietaryRestrictions().orElse(null);
            String firstName = request.firstName().orElse(null);
            String lastName = request.lastName().orElse(null);
            java.time.LocalDate dateOfBirth = request.dateOfBirth().orElse(null);
            Gender gender = request.gender().orElse(null);
            BirthNumber birthNumber = request.birthNumber()
                    .filter(s -> !s.isBlank())
                    .map(BirthNumber::of)
                    .orElse(null);

            return new Member.UpdateMemberByAdmin(
                    email, phone, address, chipNumber, null,
                    bankAccountNumber, identityCard, drivingLicenseGroup,
                    medicalCourse, trainerLicense, dietaryRestrictions, null,
                    firstName, lastName, dateOfBirth, gender, birthNumber
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }
    }
}
