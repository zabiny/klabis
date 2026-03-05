package com.klabis.members.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.members.MemberId;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;

import java.time.Instant;
import java.time.LocalDate;

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
 * @param suspensionReason    Reason for membership suspension (nullable, present only if active=false)
 * @param suspendedAt         Timestamp when membership was suspended (nullable, present only if active=false)
 * @param suspendedBy         ID of user who suspended the membership (nullable, present only if active=false)
 * @param suspensionNote      Optional note about suspension (nullable, present only if active=false)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberDetailsResponse(
        MemberId id,
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
        String dietaryRestrictions,
        String birthNumber,
        String bankAccountNumber,
        DeactivationReason suspensionReason,
        Instant suspendedAt,
        String suspendedBy,
        String suspensionNote
) {
}
