package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Contact(
        String email,
        String phone,
        String note) {
}
