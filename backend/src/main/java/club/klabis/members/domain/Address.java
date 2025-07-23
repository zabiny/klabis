package club.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Address(String streetAndNumber, String city, String postalCode, String country) {
}
