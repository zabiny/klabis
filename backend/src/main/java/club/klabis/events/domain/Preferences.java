package club.klabis.events.domain;

import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Optional;

@AggregateRoot
public class Preferences {

    @Identity
    private MemberId memberId;
    private String siCardNumber;
    private String preferredCategory;

    public static Preferences defaultPreferences(MemberId memberId) {
        return new Preferences(memberId);
    }

    public Preferences(MemberId memberId) {
        this.memberId = memberId;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public Optional<String> getPreferredCategory() {
        return Optional.ofNullable(preferredCategory);
    }

    public void setPreferredCategory(String preferredCategory) {
        this.preferredCategory = preferredCategory;
    }

    public Optional<String> getSiCardNumber() {
        return Optional.ofNullable(siCardNumber);
    }

    public void setSiCardNumber(String siCardNumber) {
        this.siCardNumber = siCardNumber;
    }
}
