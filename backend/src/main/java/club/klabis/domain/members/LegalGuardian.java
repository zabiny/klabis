package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record LegalGuardian(
        String firstName,
        String lastName,
        Contact contact,
        String note
) {

}
