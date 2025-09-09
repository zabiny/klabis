package club.klabis.events.infrastructure.inmemoryrepo;

import club.klabis.events.application.EventsRepository;
import club.klabis.events.domain.Event;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.function.Predicate;

@Repository
@SecondaryAdapter
interface InMemoryEventsRepository extends InMemoryRepository<Event, Event.Id>, EventsRepository {

    @Override
    default Page<Event> findEvents(EventsQuery filter, Pageable pageable) {
        Predicate<Event> filterPredicate = (e) -> true;

        if (filter.registeredMember() != null) {
            filterPredicate = filterPredicate.and(e -> e.isMemberRegistered(filter.registeredMember()));
        }

        return findAll(filterPredicate, pageable);
    }

}
