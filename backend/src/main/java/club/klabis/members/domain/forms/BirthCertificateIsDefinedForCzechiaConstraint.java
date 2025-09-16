package club.klabis.members.domain.forms;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

class BirthCertificateIsDefinedForCzechiaConstraint {

    static boolean isValid(String nationality, String birthCertificateNumber) {
        if (!"CZ".equals(nationality)) {
            return true;
        }

        return StringUtils.hasLength(birthCertificateNumber);
    }

    static class RegistrationFormValidator implements ConstraintValidator<BirthCertificateIsDefinedForCzechia, RegistrationForm> {

        @Override
        public boolean isValid(RegistrationForm registrationForm, ConstraintValidatorContext constraintValidatorContext) {
            return BirthCertificateIsDefinedForCzechiaConstraint.isValid(registrationForm.nationality(), registrationForm.birthCertificateNumber());
        }
    }

    static class MemberEditFormValidator implements ConstraintValidator<BirthCertificateIsDefinedForCzechia, MemberEditForm> {

        @Override
        public boolean isValid(MemberEditForm memberEditForm, ConstraintValidatorContext constraintValidatorContext) {
            return BirthCertificateIsDefinedForCzechiaConstraint.isValid(memberEditForm.nationality(), memberEditForm.birthCertificateNumber());
        }
    }

    static class EditAnotherMemberInfoByAdminFormValidator implements ConstraintValidator<BirthCertificateIsDefinedForCzechia, EditAnotherMemberInfoByAdminForm> {
        @Override
        public boolean isValid(EditAnotherMemberInfoByAdminForm memberEditForm, ConstraintValidatorContext constraintValidatorContext) {
            return BirthCertificateIsDefinedForCzechiaConstraint.isValid(memberEditForm.nationality(), memberEditForm.birthCertificateNumber());
        }
    }
}
