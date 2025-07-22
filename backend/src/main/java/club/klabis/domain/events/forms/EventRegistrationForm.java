package club.klabis.domain.events.forms;

import club.klabis.domain.members.Member;

public record EventRegistrationForm(Member.Id memberId, String siNumber) {
}
