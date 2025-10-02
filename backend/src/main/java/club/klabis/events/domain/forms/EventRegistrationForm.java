package club.klabis.events.domain.forms;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;

@RecordBuilder
public record EventRegistrationForm(@NotBlank String siNumber, @NotBlank String category) {
}
