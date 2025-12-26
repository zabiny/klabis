package club.klabis.events.application;

import club.klabis.PresetDataLoader;
import club.klabis.events.domain.EventManagementCommand;
import club.klabis.shared.config.faker.Events;
import club.klabis.shared.config.faker.KlabisFaker;
import club.klabis.shared.config.faker.KlabisFakerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

// load these data only when ORIS integration is disabled
@ConditionalOnProperty(prefix = "oris-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
@Component
class EventsPresetDataLoader implements PresetDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(EventsPresetDataLoader.class);

    private final EventCreationUseCase eventsService;
    private final KlabisFaker faker = new KlabisFakerImpl();

    public EventsPresetDataLoader(EventCreationUseCase eventsService) {
        this.eventsService = eventsService;
    }

    private EventManagementCommand randomForm(int index) {
        Events.Event randomEvent = faker.events().weightedEvent();

        return new EventManagementCommand(randomEvent.name(),
                faker.cities().weightedCity(),
                randomEvent.eventDate(),
                faker.clubs().weightedClub().code(),
                randomEvent.registrationsDeadline(),
                null, faker.categories().weightedCategories(), null);
    }

    @Override
    public void loadData() {
        Stream.iterate(1, old -> old + 1)
                .limit(30)
                .map(this::randomForm)
                .map(eventsService::createNewEvent)
                .forEach(createdEvent -> LOG.trace("Created event with ID {}", createdEvent.getId()));
    }
}
