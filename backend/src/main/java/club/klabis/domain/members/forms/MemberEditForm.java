package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collection;

@BirthCertificateIsDefinedForCzechia
@AtLeastOneContactIsDefined.List({
        @AtLeastOneContactIsDefined(contactType = ContactType.EMAIL, message = "At least one email contact must be provided"),
        @AtLeastOneContactIsDefined(contactType = ContactType.PHONE, message = "At least one phone contact must be provided")
})
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
