package club.klabis.events.application;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;

@RecordBuilder
public record OrisData(int orisId, @NotBlank String name, @NotNull LocalDate eventDate,
                       @NotNull ZonedDateTime registrationsDeadline,
                       String location, String organizer, Collection<String> categories) {

}
