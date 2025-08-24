package club.klabis.events.domain.forms;

import club.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EventRegistrationForm(MemberId memberId, String siNumber) {
}
