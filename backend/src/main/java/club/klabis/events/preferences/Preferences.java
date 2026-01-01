package club.klabis.events.preferences;

import club.klabis.members.MemberId;
import club.klabis.members.domain.RegistrationNumber;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.Optional;

@AggregateRoot
public class Preferences extends AbstractAggregateRoot<Preferences> {

    @Identity
    private MemberId memberId;
    private RegistrationNumber registrationNumber;
    private String siCardNumber;

    public static Preferences defaultPreferences(MemberId memberId) {
        return new Preferences(memberId);
    }

    public Preferences(MemberId memberId) {
        this.memberId = memberId;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public Optional<String> getSiCardNumber() {
        return Optional.ofNullable(siCardNumber);
    }

    public void setSiCardNumber(String siCardNumber) {
        this.siCardNumber = siCardNumber;
    }

    public RegistrationNumber getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(RegistrationNumber registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
