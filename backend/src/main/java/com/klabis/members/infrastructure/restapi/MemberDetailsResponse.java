package com.klabis.members.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.security.fieldsecurity.NullDeniedHandler;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.MemberId;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.time.Instant;
import java.time.LocalDate;

@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
public record MemberDetailsResponse(
        @OwnerId
        MemberId id,
        String registrationNumber,
        String firstName,
        String lastName,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        LocalDate dateOfBirth,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        String nationality,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        Gender gender,
        String email,
        String phone,
        AddressResponse address,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        GuardianDTO guardian,
        boolean active,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        String chipNumber,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        IdentityCardDto identityCard,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        MedicalCourseDto medicalCourse,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        TrainerLicenseDto trainerLicense,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        RefereeLicenseDto refereeLicense,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        DrivingLicenseGroup drivingLicenseGroup,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        String dietaryRestrictions,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        String birthNumber,
        @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
        String bankAccountNumber,
        @HasAuthority(Authority.MEMBERS_MANAGE)
        DeactivationReason suspensionReason,
        @HasAuthority(Authority.MEMBERS_MANAGE)
        Instant suspendedAt,
        @HasAuthority(Authority.MEMBERS_MANAGE)
        String suspendedBy,
        @HasAuthority(Authority.MEMBERS_MANAGE)
        String suspensionNote,
        TrainingGroupResponse trainingGroup,
        FamilyGroupResponse familyGroup
) {

    public record OwnerResponse(String fullName, String email) {}

    public record TrainingGroupResponse(String groupName, java.util.List<OwnerResponse> owners) {}

    public record FamilyGroupResponse(String groupName, java.util.List<OwnerResponse> owners) {}
}
