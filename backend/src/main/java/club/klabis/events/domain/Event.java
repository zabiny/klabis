package club.klabis.events.domain;

import club.klabis.events.domain.events.EventEditedEvent;
import club.klabis.events.domain.forms.EventEditationForm;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@AggregateRoot
public abstract class Event extends AbstractAggregateRoot<Event> {

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

        public String toString() {
            return Integer.toString(value);
        }
    }

    @Identity
    private final Id id;
    private LocalDate date;
    private String name;
    private String location;
    private String organizer;
    private ZonedDateTime registrationDeadline;
    private MemberId coordinator;
    private OrisId orisId;
    private URL website;

    private final Set<Registration> registrations = new HashSet<>();

    public Optional<MemberId> getCoordinator() {
        return Optional.ofNullable(coordinator);
    }

    public Optional<OrisId> getOrisId() {
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

    public Optional<URL> getWebsite() {
        return Optional.ofNullable(website);
    }

    public ZonedDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public boolean hasOrisId() {
        return orisId != null;
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

    public void synchronize(OrisData orisData) {
        this.name = orisData.name();
        this.location = orisData.location();
        this.organizer = orisData.organizer();
        this.date = orisData.eventDate();
        this.registrationDeadline = orisData.registrationsDeadline();
        this.website = orisData.website();
        this.linkWithOris(orisData.orisId());
    }

    public void closeRegistrations(ZonedDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Event linkWithOris(OrisId orisId) {
        this.orisId = orisId;
        return this;
    }

    public Optional<Registration> getRegistrationForMember(MemberId memberId) {
        return registrations.stream().filter(it -> Objects.equals(it.getMemberId(), memberId)).findFirst();
    }

    public boolean areRegistrationsOpen() {
        return registrationDeadline.isAfter(ZonedDateTime.now());
    }

    public boolean isMemberRegistered(MemberId memberId) {
        return registrations.stream().anyMatch(r -> memberId.equals(r.getMemberId()));
    }

    public void registerMember(MemberId memberId, EventRegistrationForm form) {
        if (!this.areRegistrationsOpen()) {
            throw new EventException(this.id,
                    "Cannot add new registration to event, registrations are already closed",
                    EventException.Type.REGISTRATION_DEADLINE_PASSED);
        }

        if (this.getRegistrationForMember(memberId).isPresent()) {
            throw EventException.createAlreadySignedUpException(this.id, memberId);
        }

        this.registrations.add(new Registration(memberId, form.siNumber(), form.category()));
    }

    public void changeRegistration(MemberId memberId, EventRegistrationForm form) {
        if (!this.areRegistrationsOpen()) {
            throw new EventException(this.id,
                    "Cannot change registration for event, registrations are already closed",
                    EventException.Type.REGISTRATION_DEADLINE_PASSED);
        }

        this.getRegistrationForMember(memberId).ifPresentOrElse(registration -> {
            registration.update(form.category(), form.siNumber());
        }, () -> {
            throw EventException.createMemberNotRegisteredForEventException(this.id, memberId);
        });
    }

    public void cancelMemberRegistration(MemberId memberId) {
        if (!areRegistrationsOpen()) {
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

