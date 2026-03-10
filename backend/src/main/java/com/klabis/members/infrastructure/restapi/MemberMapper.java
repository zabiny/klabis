package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

/**
 * MapStruct mapper for Member entity to DTO transformations.
 * <p>
 * Replaces manual mapping methods in MemberController with compile-time generated code.
 */
@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL,
        imports = MemberId.class
)
interface MemberMapper {

    /**
     * Maps Member domain object to MemberSummaryResponse.
     *
     * @param member the source member domain object
     * @return mapped summary response
     */
    @Mapping(target = "id", expression = "java(MemberId.fromUserId(member.getUserId()))")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    MemberSummaryResponse toSummaryResponse(Member member);

    /**
     * Maps Member domain object to MemberDetailsResponse.
     * <p>
     * Uses expression mappings for AddressResponse and GuardianDTO.
     *
     * @param member the source member domain object
     * @return mapped details response
     */
    default MemberDetailsResponse toDetailsResponse(Member member) {
        if (member == null) {
            return null;
        }
        MemberDetailsResponse response = toDetailsResponseInternal(member);
        return new MemberDetailsResponse(
                response.id(),
                response.registrationNumber(),
                response.firstName(),
                response.lastName(),
                response.dateOfBirth(),
                response.nationality(),
                response.gender(),
                member.getEmail() != null ? member.getEmail().value() : null,
                member.getPhone() != null ? member.getPhone().value() : null,
                response.address(),
                response.guardian(),
                response.active(),
                response.chipNumber(),
                response.identityCard(),
                response.medicalCourse(),
                response.trainerLicense(),
                response.drivingLicenseGroup(),
                response.dietaryRestrictions(),
                member.getBirthNumber() != null ? member.getBirthNumber().value() : null,
                member.getBankAccountNumber() != null ? member.getBankAccountNumber().value() : null,
                member.getSuspensionReason(),
                member.getSuspendedAt(),
                member.getSuspendedBy() != null ? member.getSuspendedBy().uuid().toString() : null,
                member.getSuspensionNote()
        );
    }

    @Mapping(target = "id", expression = "java(MemberId.fromUserId(member.getUserId()))")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "guardian", source="guardian")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
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
    @Mapping(target = "birthNumber", ignore = true)
    @Mapping(target = "bankAccountNumber", ignore = true)
    @Mapping(target = "suspendedBy", ignore = true)
    @Mapping(target = "suspendedAt", ignore = true)
    @Mapping(target = "suspensionReason", ignore = true)
    @Mapping(target = "suspensionNote", ignore = true)
    MemberDetailsResponse toDetailsResponseInternal(Member member);

    AddressResponse addressToResponse(Address address);

    default GuardianDTO guardianToResponse(GuardianInformation guardianInformation) {
        return GuardianDTO.from(guardianInformation);
    }

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

    /**
     * Maps RegisterMemberRequest to RegisterNewMember service command.
     *
     * @param request the source registration request
     * @param registeredBy the user performing the registration
     * @return mapped service command
     */
    default com.klabis.members.application.RegistrationService.RegisterNewMember toRegisterNewMemberCommand(
            RegisterMemberRequest request, UserId registeredBy) {
        return new com.klabis.members.application.RegistrationService.RegisterNewMember(
                createPersonalInformation(request.firstName(), request.lastName(),
                        request.dateOfBirth(), request.gender(), request.nationality()),
                request.address() != null ? new Address(request.address().street(), request.address().city(),
                        request.address().postalCode(), request.address().country()) : null,
                EmailAddress.of(request.email()),
                PhoneNumber.of(request.phone()),
                request.guardian() != null ? new GuardianInformation(request.guardian().firstName(),
                        request.guardian().lastName(), request.guardian().relationship(),
                        request.guardian().email(), request.guardian().phone()) : null,
                request.birthNumber() != null ? BirthNumber.of(request.birthNumber()) : null,
                request.bankAccountNumber() != null ? BankAccountNumber.of(request.bankAccountNumber()) : null,
                registeredBy
        );
    }

    default PersonalInformation createPersonalInformation(
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            String nationality
    ) {
        return PersonalInformation.of(firstName, lastName, dateOfBirth, nationality, gender);
    }
}
