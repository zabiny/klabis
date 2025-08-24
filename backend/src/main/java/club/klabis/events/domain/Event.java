package club.klabis.events.domain;

import club.klabis.events.domain.events.EventEditedEvent;
import club.klabis.events.domain.forms.EventEditationForm;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.util.*;

@AggregateRoot
public class Event extends AbstractAggregateRoot<Event> {

    protected Event() {
        id = Id.newId();
    }

    public Collection<Registration> getEventRegistrations() {
        return new HashSet<>(registrations);
    }

    public record Id(int value) {

        private static Id LAST_ID = new Id(0);

        private static Id newId() {
            LAST_ID = new Id(LAST_ID.value() + 1);
            return LAST_ID;
        }
    }

    @Identity
    private final Id id;
    private LocalDate date;
    private String name;
    private String location;
    private String organizer;
    private LocalDate registrationDeadline;
    private MemberId coordinator;
    private Integer orisId;

    private Set<Registration> registrations = new HashSet<>();

    public Optional<MemberId> getCoordinator() {
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

    public void closeRegistrations(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Event linkWithOris(int orisId) {
        this.orisId = orisId;
        return this;
    }

    private Optional<Registration> getRegistrationForMember(MemberId memberId) {
        return registrations.stream().filter(it -> Objects.equals(it.memberId(), memberId)).findFirst();
    }

    public void registerMember(EventRegistrationForm form) {
        if (this.registrationDeadline.isBefore(LocalDate.now())) {
            throw new EventException(this.id,
                    "Cannot add new registration to event, registrations are already closed",
                    EventException.Type.REGISTRATION_DEADLINE_PASSED);
        }

        if (this.getRegistrationForMember(form.memberId()).isPresent()) {
            throw EventException.createAlreadySignedUpException(this.id, form.memberId());
        }

        this.registrations.add(new Registration(form.memberId(), form.siNumber()));
    }

    public void cancelMemberRegistration(MemberId memberId) {
        if (this.registrationDeadline.isBefore(LocalDate.now())) {
            throw new EventException(this.id,
                    "Cannot remove registration from event, registrations are already closed",
                    EventException.Type.REGISTRATION_DEADLINE_PASSED);
        }

        getRegistrationForMember(memberId).ifPresentOrElse(registration -> this.registrations.remove(registration),
                () -> {
                    throw EventException.createMemberNotRegisteredForEventException(this.id, memberId);
                });
    }
}

