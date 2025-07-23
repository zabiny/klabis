package club.klabis.events.domain.forms;

import club.klabis.domain.members.Member;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDate;

@RecordBuilder
public record EventEditationForm(String name, String location, LocalDate date, String organizer,
                                 LocalDate registrationDeadline, Member.Id coordinator) {

}
