package club.klabis.events.domain;

import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public record Registration(
        @Identity
        MemberId memberId,
        String siNumber
) {
}
