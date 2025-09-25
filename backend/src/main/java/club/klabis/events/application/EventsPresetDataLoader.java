package club.klabis.events.application;

import club.klabis.PresetDataLoader;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.forms.EventEditationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
        Event createdEvent = eventsService.createNewEvent(new EventEditationForm("Example opened event",
                "Brno",
                LocalDate.now(),
                "ZBM",
                LocalDate.now()
                        .plusDays(3),
                null));
        System.out.printf("Created event with ID %s%n", createdEvent.getId());
        createdEvent = eventsService.createNewEvent(new EventEditationForm("Example passed event",
                "Jilemnice",
                LocalDate.now().minusDays(12),
                "ZBM",
                LocalDate.now()
                        .minusDays(20),
                null));
        System.out.printf("Created event with ID %s%n", createdEvent.getId());
    }
}
