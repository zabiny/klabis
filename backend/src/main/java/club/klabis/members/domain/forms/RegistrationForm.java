package club.klabis.members.domain.forms;

import club.klabis.members.domain.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collection;

@RecordBuilder
@BirthCertificateIsDefinedForCzechia
@AtLeastOneContactIsDefined.List({
        @AtLeastOneContactIsDefined(contactType = Contact.Type.EMAIL, message = "At least one email contact must be provided"),
        @AtLeastOneContactIsDefined(contactType = Contact.Type.PHONE, message = "At least one phone contact must be provided")
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
        @NotBlank
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
