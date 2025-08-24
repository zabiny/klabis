package club.klabis.events.domain.forms;

import club.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDate;

@RecordBuilder
public record EventEditationForm(String name, String location, LocalDate date, String organizer,
                                 LocalDate registrationDeadline, MemberId coordinator) {

}
