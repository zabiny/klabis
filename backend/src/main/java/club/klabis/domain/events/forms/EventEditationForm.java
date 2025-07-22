package club.klabis.domain.events.forms;

import club.klabis.domain.events.Event;
import club.klabis.domain.members.Member;

import java.time.LocalDate;

public record EventEditationForm(String name, String location, LocalDate date, String organizer, LocalDate registrationDeadline, Member.Id coordinator) {
    
    public static EventEditationForm fromEvent(Event event) {
        return new EventEditationForm(
            event.getName(),
            event.getLocation(),
            event.getDate(),
            event.getOrganizer(),
            event.getRegistrationDeadline(),
            event.getCoordinator().orElse(null)
        );
    }
    
}
