package club.klabis.events.domain;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;

@RecordBuilder
public record OrisData(OrisId orisId, @NotBlank String name, @NotNull LocalDate eventDate,
                       @NotNull ZonedDateTime registrationsDeadline,
                       String location, String organizer, @NotNull Collection<String> categories, URL website) {

}
