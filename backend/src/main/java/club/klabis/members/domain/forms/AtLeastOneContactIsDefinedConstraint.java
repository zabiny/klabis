package club.klabis.members.domain.forms;

import club.klabis.api.dto.ContactApiDto;
import club.klabis.api.dto.EditMyDetailsFormApiDto;
import club.klabis.api.dto.LegalGuardianApiDto;
import club.klabis.members.domain.Contact;
import club.klabis.members.domain.LegalGuardian;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

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

    private static boolean isValid(@Valid ContactApiDto contact, @Valid List<LegalGuardianApiDto> guardians, Contact.Type requiredContactType) {
        if (hasContactOfType(contact, requiredContactType)) {
            return true;
        }

        if (guardians != null) {
            return guardians.stream().map(LegalGuardianApiDto::getContact).anyMatch(c -> hasContactOfType(c, requiredContactType));
        }

        return false;
    }

    private static boolean hasContactOfType(Collection<Contact> contacts, Contact.Type contactType) {
        return contacts != null && contacts.stream().anyMatch(c -> contactType.equals(c.type()));
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

    static class EditMyDetailsFormApiDtoFormValidator implements ConstraintValidator<AtLeastOneContactIsDefined, EditMyDetailsFormApiDto> {
        private AtLeastOneContactIsDefined annotation;

        @Override
        public void initialize(AtLeastOneContactIsDefined constraintAnnotation) {
            this.annotation = constraintAnnotation;
        }

        @Override
        public boolean isValid(EditMyDetailsFormApiDto editForm, ConstraintValidatorContext constraintValidatorContext) {
            return AtLeastOneContactIsDefinedConstraint.isValid(editForm.getContact(), editForm.getGuardians(), annotation.contactType());
        }
    }

}
