package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface EventsRepository extends ListCrudRepository<Event, Event.Id>, PagingAndSortingRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(int orisId);

    default Collection<Event> findEventsByRegistrationsContaining(MemberId participantId) {
        return findAll().stream()
                .filter(it -> it.getEventRegistrations().stream().anyMatch(r -> r.memberId().equals(participantId)))
                .collect(
                        Collectors.toList());
    }

}
