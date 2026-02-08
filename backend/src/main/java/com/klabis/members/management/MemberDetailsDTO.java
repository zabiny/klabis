package com.klabis.members.management;

import com.klabis.members.DrivingLicenseGroup;
import com.klabis.members.Gender;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Application layer DTO containing complete member details.
 * <p>
 * This DTO transfers full member information from the domain layer to the presentation layer.
 * Includes all personal information, contact details, optional guardian information, and address.
 *
 * @param id                  Member's unique identifier
 * @param registrationNumber  Member's unique registration number in format XXXYYSS
 * @param firstName           Member's first name
 * @param lastName            Member's last name
 * @param dateOfBirth         Member's date of birth
 * @param nationality         Member's nationality code (ISO 3166-1 alpha-2)
 * @param gender              Member's gender
 * @param email               Member's email address
 * @param phone               Member's phone number in E.164 format
 * @param address             Member's postal address
 * @param guardian            Guardian information (present if member has guardian)
 * @param active              Whether the member is active
 * @param chipNumber          Member's chip number (nullable)
 * @param identityCard        Member's identity card information (nullable)
 * @param medicalCourse       Member's medical course information (nullable)
 * @param trainerLicense      Member's trainer license information (nullable)
 * @param drivingLicenseGroup Member's driving license group (nullable)
 * @param dietaryRestrictions Member's dietary restrictions (nullable, max 500 chars)
 */
record MemberDetailsDTO(
        UUID id,
        String registrationNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationality,
        Gender gender,
        String email,
        String phone,
        AddressResponse address,
        GuardianDTO guardian,
        boolean active,
        String chipNumber,
        IdentityCardDto identityCard,
        MedicalCourseDto medicalCourse,
        TrainerLicenseDto trainerLicense,
        DrivingLicenseGroup drivingLicenseGroup,
        String dietaryRestrictions
) {
    /**
     * @param id               Member's unique identifier
     * @param registrationNumber Member's unique registration number in format XXXYYSS
     * @param firstName        Member's first name
     * @param lastName         Member's last name
     * @param dateOfBirth      Member's date of birth
     * @param nationality      Member's nationality code (ISO 3166-1 alpha-2)
     * @param gender           Member's gender
     * @param email            Member's email address
     * @param phone            Member's phone number in E.164 format
     * @param address          Member's postal address
     * @param guardian         Guardian information (present if member has guardian)
     * @param active           Whether the member is active
     * @param chipNumber       Member's chip number (nullable)
     * @param identityCard     Member's identity card information (nullable)
     * @param medicalCourse    Member's medical course information (nullable)
     * @param trainerLicense   Member's trainer license information (nullable)
     * @param drivingLicenseGroup Member's driving license group (nullable)
     * @param dietaryRestrictions Member's dietary restrictions (nullable, max 500 chars)
     */
}
