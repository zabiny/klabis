package club.klabis.shared.config.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.service.WeightedRandomSelector;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Events extends AbstractProvider<BaseProviders> {
    private static final WeightedRandomSelector selector = new WeightedRandomSelector(new Random());
    private static final Random RANDOM = new Random();


    public record Event(String name, LocalDate eventDate, ZonedDateTime registrationsDeadline) {

        public Event(String name) {
            this(name, null, null);
        }

        public Event withRandomDates() {
            LocalDate eventDate = LocalDate.now().minusDays(15).plusDays(RANDOM.nextInt(50));
            ZonedDateTime registrations = eventDate.minusDays(RANDOM.nextInt(14))
                    .atStartOfDay(ZoneId.of("Europe/Prague"));

            return new Event(name, eventDate, registrations);
        }
    }

    private static final Event[] EVENTS = {
            new Event("Jihomoravska liga"),
            new Event("BZL"),
            new Event("Trenink")};
    private static final List<Map<String, Object>> WEIGHTED_INSECTS = List.of(
            Map.of("value", EVENTS[0], "weight", 6.0),
            Map.of("value", EVENTS[1], "weight", 1.0),
            Map.of("value", EVENTS[2], "weight", 3.0)
    );

    public Events(BaseProviders faker) {
        super(faker);
    }

    public Event nextEvent() {
        return EVENTS[faker.random().nextInt(EVENTS.length)].withRandomDates();
    }

    public Event weightedEvent() {
        Event result = selector.select(WEIGHTED_INSECTS);
        return result.withRandomDates();
    }
}