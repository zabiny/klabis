package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.GuardianInformation;
import com.klabis.members.domain.IdentityCard;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MedicalCourse;
import com.klabis.members.domain.RefereeLicense;
import com.klabis.members.domain.TrainerLicense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. See {@code MonetaryAmountConverter}
 * for the precedent.
 * <p>
 * Nested field mapping (address, guardian, identity card, medical course, licenses) is declared
 * directly on this interface rather than shared via {@code uses = MemberMapper.class} — {@code
 * Converter} beans are visible to every {@code @WebMvcTest} slice in the app regardless of its
 * {@code controllers} filter, so a {@code uses} dependency on a plain, non-Converter {@code @Mapper}
 * would force every unrelated slice to import that mapper's generated impl too.
 */
@Mapper(
        config = MapstructSpringMapperConfig.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
interface MemberDetailsConverter extends Converter<Member, MemberDetailsResponse> {

    @Override
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
    MemberDetailsResponse convert(Member member);

    AddressResponse addressToResponse(Address address);

    Gender genderToDto(com.klabis.members.domain.Gender gender);

    DrivingLicenseGroup drivingLicenseGroupToDto(com.klabis.members.domain.DrivingLicenseGroup drivingLicenseGroup);

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

    @Mapping(target = "validityDate", expression = "java(medicalCourse.validityDate() != null ? medicalCourse.validityDate().orElse(null) : null)")
    MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse);

    TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense);

    RefereeLicenseDto refereeLicenseToDto(RefereeLicense refereeLicense);

    TrainerLicenseDtoLevel trainerLevelToDto(com.klabis.members.domain.TrainerLevel level);

    RefereeLicenseDtoLevel refereeLevelToDto(com.klabis.members.domain.RefereeLevel level);
}
