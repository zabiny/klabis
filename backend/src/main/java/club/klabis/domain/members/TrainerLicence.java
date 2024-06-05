package club.klabis.domain.members;

import java.time.LocalDate;

public record TrainerLicence(TrainerLicenceType type, LocalDate expiryDate) {
}
