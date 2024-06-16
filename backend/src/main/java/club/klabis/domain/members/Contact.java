package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Contact(
        ContactType type,
        String value,
        String note) {
}

