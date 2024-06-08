package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collection;

// TODO : dodelat extended validace - see https://github.com/orgs/zabiny/projects/1/views/1?pane=issue&itemId=39979105
@RecordBuilder
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
