package club.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

@ValueObject
public record IdentityCard(String number, LocalDate expiryDate) {
}
