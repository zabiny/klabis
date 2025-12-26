package club.klabis.events.oris;

import club.klabis.events.domain.Event;
import club.klabis.events.oris.dto.OrisEventListFilter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;

@PrimaryPort
public interface OrisEventSynchronizationUseCase {
    void loadOrisEvents(OrisEventListFilter filter);

    void synchronizeEvents(Collection<Event.Id> eventIds);
}
