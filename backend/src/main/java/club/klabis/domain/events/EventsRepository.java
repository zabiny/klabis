package club.klabis.domain.events;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventsRepository extends ListCrudRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(int orisId);

}
