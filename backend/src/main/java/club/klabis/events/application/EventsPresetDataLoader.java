package club.klabis.events.application;

import club.klabis.PresetDataLoader;
import club.klabis.events.domain.forms.EventEditationForm;
import club.klabis.shared.config.faker.KlabisFaker;
import club.klabis.shared.config.faker.KlabisFakerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.stream.Stream;

// load these data only when ORIS integration is disabled
@ConditionalOnProperty(prefix = "oris-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
@Component
class EventsPresetDataLoader implements PresetDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(EventsPresetDataLoader.class);

    private final EventCreationUseCase eventsService;
    private final KlabisFaker faker = new KlabisFakerImpl();
    private final Random random = new Random();

    public EventsPresetDataLoader(EventCreationUseCase eventsService) {
        this.eventsService = eventsService;
    }

    private EventEditationForm randomForm(int index) {
        LocalDate eventDate = LocalDate.now().minusDays(15).plusDays(random.nextInt(50));
        ZonedDateTime registrations = eventDate.minusDays(random.nextInt(14)).atStartOfDay(ZoneId.of("Europe/Prague"));

        return new EventEditationForm(faker.events().weightedEvent().name(),
                faker.cities().weightedCity(),
                eventDate,
                faker.clubs().weightedClub().code(),
                registrations,
                null);
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
