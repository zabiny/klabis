package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Contact(
        Type type,
        String value,
        String note) {

    public enum Type {
        EMAIL, PHONE
    }
}

