package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegistrationForm(
        String firstName,
        String lastName,
        Sex sex,
        LocalDate dateOfBirth,
        String birthCertificateNumber,
        String nationality,
        Address address,
        Contact contact,
        List<LegalGuardian> guardians,
        BigDecimal siCard,
        String bankAccount,
        RegistrationNumber registrationNumber,
        Integer orisId
) {

}
