package club.klabis.domain.members;

import java.time.LocalDate;

public record RefereeLicence(RefereeLicenceType licenceType, LocalDate expiryDate) {
}
