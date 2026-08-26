package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.IdentityCard;
import com.klabis.members.domain.MedicalCourse;
import com.klabis.members.domain.RefereeLicense;
import com.klabis.members.domain.TrainerLicense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Not a production dependency — {@link MemberDetailsConverter} declares the same nested mappings
 * directly (see its class doc for why {@code uses =} isn't used across {@code Converter} beans).
 * Kept only so {@link MemberMappingTests} can unit-test these mapping methods in isolation.
 */
@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
interface MemberMapper {

    IdentityCardDto identityCardToDto(IdentityCard identityCard);

    @Mapping(target = "validityDate", expression = "java(medicalCourse.validityDate() != null ? medicalCourse.validityDate().orElse(null) : null)")
    MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse);

    TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense);

    RefereeLicenseDto refereeLicenseToDto(RefereeLicense refereeLicense);
}
