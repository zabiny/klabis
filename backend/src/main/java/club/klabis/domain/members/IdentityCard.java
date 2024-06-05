package club.klabis.domain.members;

import java.time.LocalDate;

public record IdentityCard(String number, LocalDate expiryDate) {
}
