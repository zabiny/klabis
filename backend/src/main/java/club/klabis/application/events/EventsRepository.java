package club.klabis.application.events;

import club.klabis.domain.events.Event;
import club.klabis.domain.members.Member;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface EventsRepository extends ListCrudRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(int orisId);

    Collection<Event> findEventsByRegistrationsContaining(Member.Id participantId);

}
