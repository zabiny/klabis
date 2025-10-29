package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisId;
import club.klabis.members.MemberId;
import club.klabis.shared.config.inmemorystorage.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
@SecondaryPort
public interface EventsRepository extends DataRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(OrisId orisId);

    Page<Event> findEvents(EventsQuery filter, Pageable pageable);

    record EventsQuery(MemberId registeredMember) {

        public static EventsQuery forRegisteredMember(MemberId memberId) {
            return new EventsQuery(memberId);
        }

    }

}
