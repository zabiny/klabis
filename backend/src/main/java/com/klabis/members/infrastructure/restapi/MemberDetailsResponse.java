package com.klabis.members.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.members.MemberId;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.Instant;
import java.time.LocalDate;

@RecordBuilder
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
        RefereeLicenseDto refereeLicense,
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
