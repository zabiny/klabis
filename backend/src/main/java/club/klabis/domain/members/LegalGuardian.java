package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Collection;

@ValueObject
public record LegalGuardian(
        String firstName,
        String lastName,
        Collection<Contact> contacts,
        String note
) {

}
