package club.klabis.members.domain.forms;

import club.klabis.members.domain.Contact;
import club.klabis.members.domain.LegalGuardian;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;

class AtLeastOneContactIsDefinedConstraint {

    protected static boolean isValid(Collection<Contact> contacts, Collection<LegalGuardian> guardians, Contact.Type requiredContactType) {
        if (hasContactOfType(contacts, requiredContactType)) {
            return true;
        }

        if (guardians != null) {
            Collection<Contact> guardianContacts = guardians.stream().map(LegalGuardian::contacts).flatMap(Collection::stream).toList();
            if (hasContactOfType(guardianContacts, requiredContactType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasContactOfType(Collection<Contact> contacts, Contact.Type contactType) {
        return contacts != null && contacts.stream().anyMatch(c -> contactType.equals(c.type()));
    }


    static class RegistrationFormValidator implements ConstraintValidator<AtLeastOneContactIsDefined, RegistrationForm> {
        private AtLeastOneContactIsDefined annotation;

        @Override
        public void initialize(AtLeastOneContactIsDefined constraintAnnotation) {
            this.annotation = constraintAnnotation;
        }

        @Override
        public boolean isValid(RegistrationForm registrationForm, ConstraintValidatorContext constraintValidatorContext) {
            return AtLeastOneContactIsDefinedConstraint.isValid(registrationForm.contact(), registrationForm.guardians(), annotation.contactType());
        }

    }

    static class EditOwnMemberInfoFormValidator implements ConstraintValidator<AtLeastOneContactIsDefined, EditOwnMemberInfoForm> {
        private AtLeastOneContactIsDefined annotation;

        @Override
        public void initialize(AtLeastOneContactIsDefined constraintAnnotation) {
            this.annotation = constraintAnnotation;
        }

        @Override
        public boolean isValid(EditOwnMemberInfoForm registrationForm, ConstraintValidatorContext constraintValidatorContext) {
            return AtLeastOneContactIsDefinedConstraint.isValid(registrationForm.contact(), registrationForm.guardians(), annotation.contactType());
        }

    }

    static class MemberEditFormValidator implements ConstraintValidator<AtLeastOneContactIsDefined, MemberEditForm> {
        private AtLeastOneContactIsDefined annotation;

        @Override
        public void initialize(AtLeastOneContactIsDefined constraintAnnotation) {
            this.annotation = constraintAnnotation;
        }

        @Override
        public boolean isValid(MemberEditForm editForm, ConstraintValidatorContext constraintValidatorContext) {
            return AtLeastOneContactIsDefinedConstraint.isValid(editForm.contact(), editForm.guardians(), annotation.contactType());
        }

    }

}
