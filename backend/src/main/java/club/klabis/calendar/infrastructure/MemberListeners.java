package club.klabis.calendar.infrastructure;

import club.klabis.calendar.CreateCalendarItemCommand;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.events.MemberCreatedEvent;
import club.klabis.members.domain.events.MemberEditedEvent;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Service
@Component
class MemberListeners {

    private final CalendarService calendarService;

    MemberListeners(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        handleMemberBirthday(event.getAggregate());
    }


    @EventListener(MemberEditedEvent.class)
    public void onMemberUpdated(MemberEditedEvent event) {
        handleMemberBirthday(event.getAggregate());
    }

    private void handleMemberBirthday(Member member) {
        // TODO: complete this handling
        LocalDate memberBirthDay = member.getDateOfBirth();
        if (memberBirthDay != null) {
            calendarService.createCalendarItem(CreateCalendarItemCommand.task(memberBirthDay.withYear(LocalDate.now()
                            .getYear()),
                    "%s %s - Narozeniny".formatted(member.getFirstName(), member.getLastName())));
        }

    }

}
