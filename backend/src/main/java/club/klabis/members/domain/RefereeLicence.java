package club.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

@ValueObject
public record RefereeLicence(Type licenceType, LocalDate expiryDate) {
    public enum Type {
        R1,
        R2,
        R3;
    }
}
