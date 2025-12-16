package club.klabis.events.domain;

import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static club.klabis.shared.config.Globals.toZonedDateTime;

@AggregateRoot
public abstract class Event extends AbstractAggregateRoot<Event> {

    protected Event() {
        id = Id.newId();
    }

    public Event(String name, LocalDate eventDate) {
        id = Id.newId();
        this.setName(name);
        this.setEventDate(eventDate);
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
    private ZonedDateTime eventStart;
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
        return eventStart.toLocalDate();
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

    public void setEventDate(LocalDate newDate) {
        this.eventStart = toZonedDateTime(newDate);

        if (this.registrationDeadline == null) {
            this.registrationDeadline = toZonedDateTime(newDate);
        } else if (this.registrationDeadline.isAfter(eventStart)) {
            this.registrationDeadline = eventStart.truncatedTo(ChronoUnit.DAYS);
        }
        andEvent(new EventDateChangedEvent(this));
    }

    public void setRegistrationDeadline(ZonedDateTime registrationDeadline) {
        if (registrationDeadline.isAfter(this.eventStart)) {
            throw new EventException(getId(),
                    "Cannot set registration deadline after event start",
                    EventException.Type.UNSPECIFIED);
        }
        this.registrationDeadline = registrationDeadline;
        andEvent(new EventRegistrationsDeadlineChangedEvent(this));
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public void setCoordinator(MemberId coordinator) {
        this.coordinator = coordinator;
    }

    public void closeRegistrations(ZonedDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Event linkWithOris(OrisId orisId) {
        if (this.orisId != null) {
            if (!this.orisId.equals(orisId)) {
                throw new EventException(id,
                        "Attempt to link event %s with already assigned orisId %s to another orisId %s".formatted(
                                getId(), this.orisId, orisId), EventException.Type.UNSPECIFIED);
            }
        } else {
            this.orisId = orisId;
        }
        return this;
    }

    public Event withWebsite(URL website) {
        this.website = website;
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

