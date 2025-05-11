package club.klabis.domain.events.forms;

import club.klabis.domain.members.Member;

import java.time.LocalDate;

public record EventEditationForm(String name, String location, LocalDate date, String organizer, LocalDate registrationDeadline, Member.Id coordinator) {
}
