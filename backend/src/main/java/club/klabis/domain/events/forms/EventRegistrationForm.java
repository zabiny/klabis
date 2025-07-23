package club.klabis.domain.events.forms;

import club.klabis.domain.members.Member;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EventRegistrationForm(Member.Id memberId, String siNumber) {
}
