package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

@ValueObject
public record TrainerLicence(Type type, LocalDate expiryDate) {
    public enum Type {
        T1,

        T2,

        T3
    }
}
