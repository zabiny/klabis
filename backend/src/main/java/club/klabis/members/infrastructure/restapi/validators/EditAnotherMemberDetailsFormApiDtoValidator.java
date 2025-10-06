package club.klabis.members.infrastructure.restapi.validators;

import club.klabis.members.infrastructure.restapi.dto.EditAnotherMemberDetailsFormApiDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class EditAnotherMemberDetailsFormApiDtoValidator implements ConstraintValidator<ApiValidationAnnotations.BirthCertificateIsDefinedForCzechiaForApi, EditAnotherMemberDetailsFormApiDto> {

    @Override
    public boolean isValid(EditAnotherMemberDetailsFormApiDto editAnotherMemberDetailsFormApiDto, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(editAnotherMemberDetailsFormApiDto.nationality(),
                editAnotherMemberDetailsFormApiDto.birthCertificateNumber());
    }

    private static boolean isValid(String nationality, String birthCertificateNumber) {
        if (!"CZ".equals(nationality)) {
            return true;
        }

        return StringUtils.hasLength(birthCertificateNumber);
    }
}
