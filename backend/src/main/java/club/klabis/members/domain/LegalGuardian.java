package club.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Collection;

// TODO: consider possibility to allow another Member to be used for LegalGuardian (then it would be either manually entered LegalGuardian like this or LegalGuardianMember where it would have only link to another Member + note as names and contacts are entered in Member)
@ValueObject
public record LegalGuardian(
        String firstName,
        String lastName,
        Collection<Contact> contacts,
        String note
) {

}
