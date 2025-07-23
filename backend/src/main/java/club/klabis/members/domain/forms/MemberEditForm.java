package club.klabis.members.domain.forms;

import club.klabis.members.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collection;

@BirthCertificateIsDefinedForCzechia
@AtLeastOneContactIsDefined(contactType = Contact.Type.EMAIL, message = "At least one email contact must be provided")
@AtLeastOneContactIsDefined(contactType = Contact.Type.PHONE, message = "At least one phone contact must be provided")
public record MemberEditForm(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotNull
        Sex sex,
        @NotNull
        Address address,
        @NotBlank
        String nationality,
        IdentityCard identityCard,
        String birthCertificateNumber,
        @NotNull
        LocalDate dateOfBirth,
        Collection<Contact> contact,
        Collection<LegalGuardian> guardians,
        String bankAccount,
        String siCard,
        String dietaryRestrictions,
        boolean medicCourse,
        Collection<DrivingLicence> drivingLicence
) {
}
