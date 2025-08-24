package club.klabis.events.domain;

import club.klabis.members.MemberId;

public record Registration(
        MemberId memberId,
        String siNumber
) {
}
