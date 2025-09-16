package club.klabis.members.infrastructure.restapi.validators;

import club.klabis.members.domain.Contact;
import club.klabis.members.infrastructure.restapi.dto.ContactApiDto;
import club.klabis.members.infrastructure.restapi.dto.EditMyDetailsFormApiDto;
import club.klabis.members.infrastructure.restapi.dto.LegalGuardianApiDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class EditMyDetailsFormApiDtoFormValidator implements ConstraintValidator<ApiValidationAnnotations.AtLeastOneContactIsDefinedForApi, EditMyDetailsFormApiDto> {
    private ApiValidationAnnotations.AtLeastOneContactIsDefinedForApi annotation;

    @Override
    public void initialize(ApiValidationAnnotations.AtLeastOneContactIsDefinedForApi constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(EditMyDetailsFormApiDto editForm, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(editForm.getContact(), editForm.getGuardians(), annotation.contactType());
    }

    private static boolean isValid(@Valid ContactApiDto contact, @Valid List<LegalGuardianApiDto> guardians, Contact.Type requiredContactType) {
        if (hasContactOfType(contact, requiredContactType)) {
            return true;
        }

        if (guardians != null) {
            return guardians.stream()
                    .map(LegalGuardianApiDto::getContact)
                    .anyMatch(c -> hasContactOfType(c, requiredContactType));
        }

        return false;
    }

    private static boolean hasContactOfType(ContactApiDto contacts, Contact.Type contactType) {
        if (contacts == null) {
            return false;
        }
        return switch (contactType) {
            case EMAIL -> StringUtils.isNotBlank(contacts.getEmail());
            case PHONE -> StringUtils.isNotBlank(contacts.getPhone());
        };
    }
}
