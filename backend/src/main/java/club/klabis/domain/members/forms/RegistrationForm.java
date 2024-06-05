package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RecordBuilder
public record RegistrationForm(
        String firstName,
        String lastName,
        Sex sex,
        LocalDate dateOfBirth,
        String birthCertificateNumber,
        String nationality,
        Address address,
        Collection<Contact> contact,
        List<LegalGuardian> guardians,
        String siCard,
        String bankAccount,
        RegistrationNumber registrationNumber,
        Integer orisId
) {

}
