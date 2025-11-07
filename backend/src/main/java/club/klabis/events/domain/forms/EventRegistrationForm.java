package club.klabis.events.domain.forms;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import org.springframework.hateoas.InputType;

@RecordBuilder
public record EventRegistrationForm(@NotBlank String siNumber,
                                    @NotBlank @InputType("radio") String category) {
}
