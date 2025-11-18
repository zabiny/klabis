package club.klabis.events.domain.forms;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import org.springframework.hateoas.InputType;

@RecordBuilder
public record EventRegistrationForm(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY) String eventName,
        @NotBlank String siNumber,
        @NotBlank @InputType("radio") String category) {
}
