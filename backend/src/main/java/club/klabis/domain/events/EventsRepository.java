package club.klabis.domain.events;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
public interface EventsRepository extends InMemoryRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(int orisId);

}
