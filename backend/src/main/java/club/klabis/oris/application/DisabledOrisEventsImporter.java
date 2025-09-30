package club.klabis.oris.application;

import club.klabis.events.domain.Event;
import club.klabis.oris.application.dto.OrisEventListFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Service is used when ORIS integration is disabled. That is needed for OpenAPI SpringDoc generator to include also ORIS APIs while avoiding unnecessary data synchronization when APP starts in that task.
 */
@Service
@ConditionalOnProperty(prefix = "oris-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledOrisEventsImporter implements OrisEventsImporter {
    @Override
    public void loadOrisEvents(OrisEventListFilter filter) {

    }

    @Override
    public void synchronizeEvents(Collection<Event.Id> eventIds) {

    }
}
