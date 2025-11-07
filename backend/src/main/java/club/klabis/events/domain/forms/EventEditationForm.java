package club.klabis.events.domain.forms;

import club.klabis.events.domain.Competition;
import club.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

@RecordBuilder
public record EventEditationForm(@NotBlank String name, String location, @NotNull LocalDate date, String organizer,
                                 @NotNull ZonedDateTime registrationDeadline, MemberId coordinator,
                                 Set<Competition.Category> categories) {

}
