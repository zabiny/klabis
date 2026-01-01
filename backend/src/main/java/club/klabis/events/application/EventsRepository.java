package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisEventId;
import club.klabis.members.MemberId;
import club.klabis.shared.application.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@SecondaryPort
public interface EventsRepository extends DataRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(OrisEventId orisId);

    Page<Event> findEvents(EventsQuery filter, Pageable pageable);

    record EventsQuery(MemberId registeredMember, Event.Id eventId, LocalDate before, LocalDate after,
                       Boolean onlyOpenedRegistrations) {

        public static EventsQuery query() {
            return new EventsQuery(null, null, null, null, false);
        }

        public EventsQuery forRegisteredMember(MemberId memberId) {
            return new EventsQuery(memberId, null, null, null, onlyOpenedRegistrations);
        }

        public EventsQuery withEventId(Event.Id eventId) {
            return new EventsQuery(registeredMember, eventId, before, after, onlyOpenedRegistrations);
        }

        public EventsQuery withOpenedRegistrations(boolean openedRegistrations) {
            return new EventsQuery(registeredMember, eventId, before, after, openedRegistrations);
        }

        public EventsQuery withDateAfter(LocalDate date) {
            return new EventsQuery(registeredMember, eventId, before, date, onlyOpenedRegistrations);
        }

        public EventsQuery withDateBefore(LocalDate date) {
            return new EventsQuery(registeredMember, eventId, date, after, onlyOpenedRegistrations);
        }

    }

}
