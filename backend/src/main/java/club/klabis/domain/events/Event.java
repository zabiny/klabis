package club.klabis.domain.events;

import club.klabis.domain.events.events.EventEditedEvent;
import club.klabis.domain.events.forms.EventEditationForm;
import club.klabis.domain.events.forms.EventRegistrationForm;
import club.klabis.domain.members.Member;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@AggregateRoot
public class Event extends AbstractAggregateRoot<Event> {

    protected Event() {
        id = Id.newId();
    }

    public Collection<Member.Id> getParticipants() {
        return participants;
    }

    public record Id(int value) {

        private static Id LAST_ID = new Id(0);

        private static Id newId() {
            LAST_ID = new Id(LAST_ID.value() + 1);
            return LAST_ID;
        }
    }

    private final Id id;
    private LocalDate date;
    private String name;
    private String location;
    private String organizer;
    private LocalDate registrationDeadline;
    private Member.Id coordinator;
    private Integer orisId;

    private Collection<Member.Id> participants = new HashSet<>();

    public Optional<Member.Id> getCoordinator() {
        return Optional.ofNullable(coordinator);
    }

    public Optional<Integer> getOrisId() {
        return Optional.ofNullable(orisId);
    }

    public LocalDate getDate() {
        return date;
    }

    public Id getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getOrganizer() {
        return organizer;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public static Event newEvent(EventEditationForm form) {
        Event event = new Event();
        event.edit(form);
        return event;
    }

    public void edit(EventEditationForm form) {
        this.date = form.date();
        this.name = form.name();
        this.location = form.location();
        this.organizer = form.organizer();
        this.registrationDeadline = form.registrationDeadline();
        this.coordinator = form.coordinator();

        this.andEvent(new EventEditedEvent(this));
    }

    public void linkWithOris(int orisId) {
        this.orisId = orisId;
    }

    public void addEventRegistration(EventRegistrationForm form) {
        if (this.participants.contains(form.memberId())) {
            throw EventException.createAlreadySignedUpException(this.id, form.memberId());
        }

        this.participants.add(form.memberId());
    }

    public void removeEventRegistration(Member.Id memberId) {
        if (!this.participants.contains(memberId)) {
            throw EventException.createMemberNotRegisteredForEventException(this.id, memberId);
        }

        this.participants.remove(memberId);
    }
}
