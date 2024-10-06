package club.klabis.domain.members.forms;

import club.klabis.domain.members.*;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

@AtLeastOneContactIsDefined(contactType = Contact.Type.EMAIL, message = "At least one email contact must be provided")
@AtLeastOneContactIsDefined(contactType = Contact.Type.PHONE, message = "At least one phone contact must be provided")
public record EditOwnMemberInfoForm(
        IdentityCard identityCard,
        String nationality,
        Address address,
        Collection<Contact> contact,
        @Valid
        List<@Valid LegalGuardian> guardians,
        String siCard,
        String bankAccount,
        String dietaryRestrictions,
        @Valid
        List<@Valid DrivingLicence> drivingLicence,
        Boolean medicCourse
) {
}
