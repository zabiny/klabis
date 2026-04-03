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

    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "email", expression = "java(member.getEmail() != null ? member.getEmail().value() : null)")
    MemberSummaryResponse toSummaryResponse(Member member);

    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "guardian", source = "guardian")
    @Mapping(target = "email", expression = "java(member.getEmail() != null ? member.getEmail().value() : null)")
    @Mapping(target = "phone", expression = "java(member.getPhone() != null ? member.getPhone().value() : null)")
    @Mapping(target = "birthNumber", expression = "java(member.getBirthNumber() != null ? member.getBirthNumber().value() : null)")
    @Mapping(target = "bankAccountNumber", expression = "java(member.getBankAccountNumber() != null ? member.getBankAccountNumber().value() : null)")
    @Mapping(target = "suspendedBy", expression = "java(member.getSuspendedBy() != null ? member.getSuspendedBy().uuid().toString() : null)")
    MemberDetailsResponse toDetailsResponse(Member member);

    AddressResponse addressToResponse(Address address);

    default GuardianDTO guardianToResponse(GuardianInformation guardianInformation) {
        return GuardianDTO.from(guardianInformation);
    }

    IdentityCardDto identityCardToDto(IdentityCard identityCard);

    MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse);

    TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense);

    RefereeLicenseDto refereeLicenseToDto(RefereeLicense refereeLicense);

    default RegistrationPort.RegisterNewMember toRegisterNewMemberCommand(
            RegisterMemberRequest request, UserId registeredBy) {
        return new RegistrationPort.RegisterNewMember(
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
