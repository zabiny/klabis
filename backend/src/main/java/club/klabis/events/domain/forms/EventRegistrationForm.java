package club.klabis.events.domain.forms;

import club.klabis.members.domain.Member;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EventRegistrationForm(Member.Id memberId, String siNumber) {
}
