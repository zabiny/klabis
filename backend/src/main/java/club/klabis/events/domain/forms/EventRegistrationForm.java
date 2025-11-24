package club.klabis.events.domain.forms;

import club.klabis.shared.config.hateoas.KlabisInputTypes;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import org.springframework.hateoas.InputType;

@RecordBuilder
public record EventRegistrationForm(
        @NotBlank String siNumber,
        @NotBlank @InputType(KlabisInputTypes.RADIO_INPUT_TYPE) String category) {
}
