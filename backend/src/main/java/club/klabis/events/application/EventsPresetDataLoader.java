package club.klabis.events.application;

import club.klabis.PresetDataLoader;
import club.klabis.events.domain.forms.EventEditationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

// load these data only when ORIS integration is disabled
@ConditionalOnProperty(prefix = "oris-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
@Component
class EventsPresetDataLoader implements PresetDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(EventsPresetDataLoader.class);

    private final EventCreationUseCase eventsService;

    public EventsPresetDataLoader(EventCreationUseCase eventsService) {
        this.eventsService = eventsService;
    }

    @Override
    public void loadData() {
        Stream.of(
                        new EventEditationForm("Example opened event",
                                "Brno",
                                LocalDate.now(),
                                "ZBM",
                                ZonedDateTime.now()
                                        .plusDays(3),
                                null),
                        new EventEditationForm("Example passed event",
                                "Jilemnice",
                                LocalDate.now().minusDays(12),
                                "ZBM",
                                ZonedDateTime.now()
                                        .minusDays(20),
                                null)
                )
                .map(eventsService::createNewEvent)
                .forEach(createdEvent -> LOG.info("Created event with ID {}", createdEvent.getId()));
    }
}
