package com.klabis.members.management;

import com.klabis.members.*;
import org.springframework.stereotype.Component;

/**
 * Utility class for MapStruct custom mappings in Member module.
 * <p>
 * Contains helper methods for complex nested object transformations
 * that cannot be expressed directly in MapStruct mappings.
 */
@Component
public class MemberMapperUtil {

    /**
     * Maps Address value object to AddressResponse DTO.
     *
     * @param address the source address
     * @return AddressResponse, or null if source is null
     */
    public AddressResponse addressToResponse(Address address) {
        return AddressResponse.from(address);
    }

    /**
     * Maps GuardianInformation value object to GuardianDTO.
     *
     * @param guardian the source guardian information
     * @return GuardianDTO, or null if source is null
     */
    public GuardianDTO guardianToDto(GuardianInformation guardian) {
        return GuardianDTO.from(guardian);
    }

    /**
     * Maps IdentityCard value object to IdentityCardDto.
     *
     * @param identityCard the source identity card
     * @return IdentityCardDto, or null if source is null
     */
    public IdentityCardDto identityCardToDto(IdentityCard identityCard) {
        if (identityCard == null) {
            return null;
        }
        return new IdentityCardDto(
                identityCard.cardNumber(),
                identityCard.validityDate()
        );
    }

    /**
     * Maps MedicalCourse value object to MedicalCourseDto.
     *
     * @param medicalCourse the source medical course
     * @return MedicalCourseDto, or null if source is null
     */
    public MedicalCourseDto medicalCourseToDto(MedicalCourse medicalCourse) {
        if (medicalCourse == null) {
            return null;
        }
        return new MedicalCourseDto(
                medicalCourse.completionDate(),
                medicalCourse.validityDate()
        );
    }

    /**
     * Maps TrainerLicense value object to TrainerLicenseDto.
     *
     * @param trainerLicense the source trainer license
     * @return TrainerLicenseDto, or null if source is null
     */
    public TrainerLicenseDto trainerLicenseToDto(TrainerLicense trainerLicense) {
        if (trainerLicense == null) {
            return null;
        }
        return new TrainerLicenseDto(
                trainerLicense.licenseNumber(),
                trainerLicense.validityDate()
        );
    }
}
