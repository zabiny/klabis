package club.klabis.members.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jmolecules.ddd.annotation.ValueObject;

import java.text.Collator;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ValueObject
public final class RegistrationNumber implements Comparable<RegistrationNumber> {

    private static final Pattern REGISTRATION_NUMBER_FORMAT = Pattern.compile("([A-Z]{3})([0-9]{2})([0-9]{2})");
    private final String club;
    private final int yearOfBirth;
    private final int yearOrder;

    public RegistrationNumber(String club, LocalDate birthDate, int yearOrder) {
        this(club, birthDate.getYear() % 100, yearOrder);
    }

    private RegistrationNumber(String club, int birthYear, int yearOrder) {
        checkYear(birthYear);

        this.club = club;
        this.yearOfBirth = birthYear;
        this.yearOrder = yearOrder;
    }

    private static void checkYear(int year) {
        if (year < 0 || year > 100) {
            throw new IllegalArgumentException("Unexpected year value, it is expected to last 2 digits of birth year (so 0-99), got %s".formatted(year));
        }
    }

    public RegistrationNumber followingRegistrationNumber() {
        return new RegistrationNumber(this.club, this.yearOfBirth, this.yearOrder + 1);
    }

    public static RegistrationNumber ofZbmClub(LocalDate birthDate, int orderInYear) {
        return new RegistrationNumber("ZBM", birthDate, orderInYear);
    }

    @JsonCreator
    public static RegistrationNumber ofRegistrationId(String registration) {
        Matcher registrationNumberMatcher = REGISTRATION_NUMBER_FORMAT.matcher(registration);
        if (registrationNumberMatcher.matches()) {
            return new RegistrationNumber(registrationNumberMatcher.group(1), Integer.parseInt(registrationNumberMatcher.group(2)), Integer.parseInt(registrationNumberMatcher.group(3)));
        } else {
            throw new IllegalArgumentException("Value %s is not valid registration number".formatted(registration));
        }
    }

    @JsonValue
    public String toRegistrationId() {
        return "%s%02d%02d".formatted(club, yearOfBirth, yearOrder);
    }

    public String toString() {
        return toRegistrationId();
    }

    public boolean isValidForBirthdate(LocalDate birthdate) {
        return birthdate.getYear() % 100 == this.yearOfBirth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RegistrationNumber) obj;
        return Objects.equals(this.club, that.club) &&
               this.yearOfBirth == that.yearOfBirth &&
               this.yearOrder == that.yearOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(club, yearOfBirth, yearOrder);
    }

    @Override
    public int compareTo(RegistrationNumber o) {
        if (o == null) {
            return -1;
        }
        return Collator.getInstance().compare(this.toRegistrationId(), o.toRegistrationId());
    }
}
