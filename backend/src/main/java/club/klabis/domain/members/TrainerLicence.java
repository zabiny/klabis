package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

@ValueObject
public record TrainerLicence(TrainerLicenceType type, LocalDate expiryDate) {
}
