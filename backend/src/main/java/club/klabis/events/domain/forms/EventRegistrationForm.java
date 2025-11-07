package club.klabis.events.domain.forms;

import club.klabis.shared.config.hateoas.forms.InputOptions;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import org.springframework.hateoas.InputType;

@RecordBuilder
public record EventRegistrationForm(@NotBlank String siNumber,
                                    @NotBlank @InputOptions @InputType("radio") String category) {
}
