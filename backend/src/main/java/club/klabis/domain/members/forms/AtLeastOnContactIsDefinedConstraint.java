package club.klabis.domain.members.forms;

import club.klabis.domain.members.Contact;
import club.klabis.domain.members.ContactType;
import club.klabis.domain.members.LegalGuardian;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;

class AtLeastOnContactIsDefinedConstraint {

    protected static boolean isValid(Collection<Contact> contacts, Collection<LegalGuardian> guardians, ContactType requiredContactType) {
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

    private static boolean hasContactOfType(Collection<Contact> contacts, ContactType contactType) {
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
            return AtLeastOnContactIsDefinedConstraint.isValid(registrationForm.contact(), registrationForm.guardians(), annotation.contactType());
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
            return AtLeastOnContactIsDefinedConstraint.isValid(editForm.contact(), editForm.guardians(), annotation.contactType());
        }

    }

}
