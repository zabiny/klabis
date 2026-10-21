package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.members.domain.Address;
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

import java.util.Optional;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. See {@code MonetaryAmountConverter}
 * for the precedent.
 * <p>
 * Nested field mapping (address, guardian, identity card, medical course, licenses) is declared
 * directly on this interface rather than shared via {@code uses =} — {@code Converter} beans are
 * visible to every {@code @WebMvcTest} slice in the app regardless of its {@code controllers}
 * filter, so a {@code uses} dependency on a plain, non-Converter {@code @Mapper} would force every
 * unrelated slice to import that mapper's generated impl too.
 */
@Mapper(
        config = MapstructSpringMapperConfig.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
interface MemberDetailsConverter extends Converter<Member, MemberDetailsResponse> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "email", source = "email.value")
    @Mapping(target = "phone", source = "phone.value")
    @Mapping(target = "birthNumber", source = "birthNumber.value")
    @Mapping(target = "bankAccountNumber", source = "bankAccountNumber.value")
    @Mapping(target = "suspendedBy", source = "suspendedBy.uuid")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "drivingLicenseGroup", source = "drivingLicenseGroup")
    @Mapping(target = "suspensionReason", source = "suspensionReason")
    @Mapping(target = "missingData", ignore = true)
    @Mapping(target = "displayContact", ignore = true)
    MemberDetailsResponse convert(Member member);

    AddressResponse addressToResponse(Address address);

    Gender genderToDto(com.klabis.members.domain.Gender gender);

    DrivingLicenseGroup drivingLicenseGroupToDto(com.klabis.members.domain.DrivingLicenseGroup drivingLicenseGroup);

    IdentityCardDto identityCardToDto(IdentityCard identityCard);

    MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse);

    default <T> T unwrap(Optional<T> value) {
        return value == null ? null : value.orElse(null);
    }

    TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense);

    RefereeLicenseDto refereeLicenseToDto(RefereeLicense refereeLicense);

    TrainerLicenseDtoLevel trainerLevelToDto(com.klabis.members.domain.TrainerLevel level);

    RefereeLicenseDtoLevel refereeLevelToDto(com.klabis.members.domain.RefereeLevel level);
}
