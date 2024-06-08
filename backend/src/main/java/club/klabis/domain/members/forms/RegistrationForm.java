package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collection;

@RecordBuilder
@BirthCertificateIsDefinedForCzechia
@AtLeastOneContactIsDefined.List({
        @AtLeastOneContactIsDefined(contactType = ContactType.EMAIL, message = "At least one email contact must be provided"),
        @AtLeastOneContactIsDefined(contactType = ContactType.PHONE, message = "At least one phone contact must be provided")
})
public record RegistrationForm(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotNull
        Sex sex,
        @NotNull
        LocalDate dateOfBirth,
        String birthCertificateNumber,
        @NotNull
        String nationality,
        @NotNull
        Address address,
        Collection<Contact> contact,
        Collection<LegalGuardian> guardians,
        String siCard,
        String bankAccount,
        @NotNull
        RegistrationNumber registrationNumber,
        Integer orisId
) {

}
