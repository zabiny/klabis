package club.klabis.events.domain.forms;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EventRegistrationForm(String siNumber, String category) {
}
