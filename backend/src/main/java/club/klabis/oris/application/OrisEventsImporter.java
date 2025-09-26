package club.klabis.oris.application;

import club.klabis.events.domain.Event;
import club.klabis.oris.application.dto.OrisEventListFilter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;

@PrimaryPort
public interface OrisEventsImporter {
    void loadOrisEvents(OrisEventListFilter filter);

    void synchronizeEvents(Collection<Event.Id> eventIds);
}
