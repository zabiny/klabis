package com.klabis.members.management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.members.DrivingLicenseGroup;
import com.klabis.members.Gender;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST API response model for complete member details.
 * <p>
 * This is the presentation layer DTO that maps 1:1 to the JSON response structure
 * for the GET /api/members/{id} endpoint. Uses Jackson annotations for serialization.
 *
 * @param id                  Member's unique identifier
 * @param registrationNumber  Member's unique registration number in format XXXYYSS
 * @param firstName           Member's first name
 * @param lastName            Member's last name
 * @param dateOfBirth         Member's date of birth (ISO 8601 format)
 * @param nationality         Member's nationality code (ISO 3166-1 alpha-2)
 * @param gender              Member's gender
 * @param email               Member's email address
 * @param phone               Member's phone number
 * @param address             Member's postal address
 * @param guardian            Guardian information (present if member has guardian)
 * @param active              Whether the member is active
 * @param chipNumber          Member's chip number (nullable, numeric only)
 * @param identityCard        Member's identity card information (nullable)
 * @param medicalCourse       Member's medical course information (nullable)
 * @param trainerLicense      Member's trainer license information (nullable)
 * @param drivingLicenseGroup Member's driving license group (nullable)
 * @param dietaryRestrictions Member's dietary restrictions (nullable, max 500 chars)
 * @param birthNumber         Member's birth number (nullable, Czech nationals only)
 * @param bankAccountNumber   Member's bank account number (nullable)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record MemberDetailsResponse(
        UUID id,
        String registrationNumber,
        String firstName,
        String lastName,
        //@JsonFormat(pattern = "yyyy-MM-dd")
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
        String dietaryRestrictions,
        String birthNumber,
        String bankAccountNumber
) {
}
