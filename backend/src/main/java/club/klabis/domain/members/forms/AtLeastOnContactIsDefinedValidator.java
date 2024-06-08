package club.klabis.domain.members.forms;

import club.klabis.domain.members.Contact;
import club.klabis.domain.members.ContactType;
import club.klabis.domain.members.LegalGuardian;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;

class AtLeastOnContactIsDefinedValidator implements ConstraintValidator<AtLeastOneContactIsDefined, RegistrationForm> {

    private AtLeastOneContactIsDefined annotation;

    @Override
    public void initialize(AtLeastOneContactIsDefined constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(RegistrationForm registrationForm, ConstraintValidatorContext constraintValidatorContext) {

        if (hasContactOfType(registrationForm.contact(), this.annotation.contactType())) {
            return true;
        }

        if (registrationForm.guardians() != null) {
            Collection<Contact> guardianContacts = registrationForm.guardians().stream().map(LegalGuardian::contacts).flatMap(Collection::stream).toList();
            if (hasContactOfType(guardianContacts, annotation.contactType())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasContactOfType(Collection<Contact> contacts, ContactType contactType) {
        return contacts != null && contacts.stream().anyMatch(c -> contactType.equals(c.type()));
    }
}
