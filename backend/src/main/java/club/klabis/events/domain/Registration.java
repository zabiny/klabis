package club.klabis.events.domain;

import club.klabis.members.domain.Member;

public record Registration(
        Member.Id memberId,
        String siNumber
) {
}
