package club.klabis.domain.members.forms;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class BirthCertificateIsDefinedForCzechiaValidator implements ConstraintValidator<BirthCertificateIsDefinedForCzechia, RegistrationForm> {
    @Override
    public boolean isValid(RegistrationForm registrationForm, ConstraintValidatorContext constraintValidatorContext) {
        if (!"CZ".equals(registrationForm.nationality())) {
            return true;
        }

        return StringUtils.hasLength(registrationForm.birthCertificateNumber());
    }
}
