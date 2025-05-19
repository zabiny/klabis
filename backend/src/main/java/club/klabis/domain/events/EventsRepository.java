package club.klabis.domain.events;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

@Repository
public interface EventsRepository extends ListCrudRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(int orisId);

}
