package com.klabis.members.management;

import com.klabis.members.*;
import com.klabis.users.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

/**
 * MapStruct mapper for Member entity to DTO transformations.
 * <p>
 * Replaces manual mapping methods in MemberController with compile-time generated code.
 */
@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface MemberMapper {

    /**
     * Maps Member domain object to MemberSummaryResponse.
     *
     * @param member the source member domain object
     * @return mapped summary response
     */
    @Mapping(target = "id", source = "id.uuid")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    MemberSummaryResponse toSummaryResponse(Member member);

    default UUID map(UserId userId) {
        return userId.uuid();
    }

    /**
     * Maps Member domain object to MemberDetailsResponse.
     * <p>
     * Uses expression mappings for AddressResponse and GuardianDTO.
     *
     * @param member the source member domain object
     * @return mapped details response
     */
    @Mapping(target = "id", source = "id.uuid")
    @Mapping(target = "registrationNumber", source = "registrationNumber")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "guardian", source="guardian")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "chipNumber", source = "chipNumber")
    @Mapping(target = "identityCard", source = "identityCard")
    @Mapping(target = "medicalCourse", source = "medicalCourse")
    @Mapping(target = "trainerLicense", source = "trainerLicense")
    @Mapping(target = "drivingLicenseGroup", source = "drivingLicenseGroup")
    @Mapping(target = "dietaryRestrictions", source = "dietaryRestrictions")
    MemberDetailsResponse toDetailsResponse(Member member);

    AddressResponse addressToResponse(Address address);

    default String emailToResponse(EmailAddress email) {
        return email.value();
    }

    default String registrationNumberToString(RegistrationNumber registrationNumber) {
        return registrationNumber.getValue();
    }

    default String phoneNumberToString(PhoneNumber phoneNumber) {
        return phoneNumber.value();
    }

    GuardianDTO guardianToResponse(GuardianInformation guardianInformation);

    /**
     * Maps IdentityCard domain object to IdentityCardDto.
     *
     * @param identityCard the source identity card
     * @return mapped DTO, or null if source is null
     */
    @Mapping(target = "cardNumber", source = "cardNumber")
    @Mapping(target = "validityDate", source = "validityDate")
    IdentityCardDto identityCardToDto(IdentityCard identityCard);

    /**
     * Maps MedicalCourse domain object to MedicalCourseDto.
     *
     * @param medicalCourse the source medical course
     * @return mapped DTO, or null if source is null
     */
    @Mapping(target = "completionDate", source = "completionDate")
    @Mapping(target = "validityDate", source = "validityDate")
    MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse);

    /**
     * Maps TrainerLicense domain object to TrainerLicenseDto.
     *
     * @param trainerLicense the source trainer license
     * @return mapped DTO, or null if source is null
     */
    @Mapping(target = "licenseNumber", source = "licenseNumber")
    @Mapping(target = "validityDate", source = "validityDate")
    TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense);
}
