package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;

import java.time.LocalDate;
import java.util.Collection;

public record MemberEditForm(
        String firstName,
        String lastName,
        IdentityCard identityCard,
        String nationality,
        Address address,
        Collection<Contact> contact,
        Collection<LegalGuardian> guardians,
        String siCard,
        String bankAccount,
        String dietaryRestrictions,
        Collection<DrivingLicence> drivingLicence,
        boolean medicCourse,
        LocalDate dateOfBirth,
        String birthCertificateNumber,
        Sex sex
) {
}
