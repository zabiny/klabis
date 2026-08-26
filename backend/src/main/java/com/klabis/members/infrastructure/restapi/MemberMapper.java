package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
interface MemberMapper {

    @Mapping(target = "id", expression = "java(member.getId().value())")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "email", expression = "java(member.getEmail() != null ? member.getEmail().value() : null)")
    MemberSummaryResponse toSummaryResponse(Member member);

    @Mapping(target = "id", expression = "java(member.getId().value())")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "guardian", source = "guardian")
    @Mapping(target = "email", expression = "java(member.getEmail() != null ? member.getEmail().value() : null)")
    @Mapping(target = "phone", expression = "java(member.getPhone() != null ? member.getPhone().value() : null)")
    @Mapping(target = "birthNumber", expression = "java(member.getBirthNumber() != null ? member.getBirthNumber().value() : null)")
    @Mapping(target = "bankAccountNumber", expression = "java(member.getBankAccountNumber() != null ? member.getBankAccountNumber().value() : null)")
    @Mapping(target = "suspendedBy", expression = "java(member.getSuspendedBy() != null ? member.getSuspendedBy().uuid().toString() : null)")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "drivingLicenseGroup", source = "drivingLicenseGroup")
    @Mapping(target = "suspensionReason", source = "suspensionReason")
    MemberDetailsResponse toDetailsResponse(Member member);

    AddressResponse addressToResponse(Address address);

    Gender genderToDto(com.klabis.members.domain.Gender gender);

    com.klabis.members.domain.Gender genderToDomain(Gender gender);

    DrivingLicenseGroup drivingLicenseGroupToDto(com.klabis.members.domain.DrivingLicenseGroup drivingLicenseGroup);

    DeactivationReason deactivationReasonToDto(com.klabis.members.domain.DeactivationReason reason);

    com.klabis.members.domain.DeactivationReason deactivationReasonToDomain(DeactivationReason reason);

    default GuardianDTO guardianToResponse(GuardianInformation guardian) {
        if (guardian == null) {
            return null;
        }
        return new GuardianDTO(
                guardian.getEmail().value(),
                guardian.getFirstName(),
                guardian.getLastName(),
                guardian.getPhone().value(),
                guardian.getRelationship()
        );
    }

    IdentityCardDto identityCardToDto(IdentityCard identityCard);

    MonetaryAmount monetaryAmountToDto(com.klabis.members.MonetaryAmount monetaryAmount);

    @Mapping(target = "validityDate", expression = "java(medicalCourse.validityDate() != null ? medicalCourse.validityDate().orElse(null) : null)")
    MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse);

    TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense);

    RefereeLicenseDto refereeLicenseToDto(RefereeLicense refereeLicense);

    TrainerLicenseDtoLevel trainerLevelToDto(com.klabis.members.domain.TrainerLevel level);

    RefereeLicenseDtoLevel refereeLevelToDto(com.klabis.members.domain.RefereeLevel level);

    default RegistrationPort.RegisterNewMember toRegisterNewMemberCommand(
            RegisterMemberRequest request, UserId registeredBy) {
        return new RegistrationPort.RegisterNewMember(
                createPersonalInformation(request.firstName(), request.lastName(),
                        request.dateOfBirth(), genderToDomain(request.gender()), request.nationality()),
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
            com.klabis.members.domain.Gender gender,
            String nationality
    ) {
        return PersonalInformation.of(firstName, lastName, dateOfBirth, nationality, gender);
    }
}
