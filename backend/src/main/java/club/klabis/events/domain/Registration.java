package club.klabis.events.domain;

import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
public final class Registration {
    @Identity
    private final MemberId memberId;
    private String siNumber;    // TODO: create value object

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public MemberId getMemberId() {
        return memberId;
    }

    public String getSiNumber() {
        return siNumber;
    }

    public Registration(
            MemberId memberId, String siNumber
    ) {
        this.memberId = memberId;
        this.siNumber = siNumber;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public void setSiNumber(String siNumber) {
        this.siNumber = siNumber;
        this.updatedAt = ZonedDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Registration) obj;
        return Objects.equals(this.memberId, that.memberId) &&
               Objects.equals(this.siNumber, that.siNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, siNumber);
    }

    @Override
    public String toString() {
        return "Registration[" +
               "memberId=" + memberId + ", " +
               "siNumber=" + siNumber + ']';
    }

}
