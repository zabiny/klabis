package club.klabis.events.application;

import club.klabis.events.domain.Event;
import club.klabis.members.MemberId;
import com.dpolach.inmemoryrepository.PageUtils;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public interface EventsRepository extends ListCrudRepository<Event, Event.Id>, PagingAndSortingRepository<Event, Event.Id> {

    Optional<Event> findByOrisId(int orisId);

    default Page<Event> findEvents(EventsFilter filter, Pageable pageable) {
        Predicate<Event> filterPredicate = (e) -> true;

        if (filter.registeredMember() != null) {
            filterPredicate = filterPredicate.and(e -> e.isMemberRegistered(filter.registeredMember()));
        }

        List<Event> allData = findAll().stream()
                .filter(filterPredicate)
                .collect(Collectors.toList());

        return PageUtils.create(allData, pageable);
    }

    record EventsFilter(MemberId registeredMember) {

        public static EventsFilter forRegisteredMember(MemberId memberId) {
            return new EventsFilter(memberId);
        }

    }

}
