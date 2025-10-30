package club.klabis.shared.config.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.service.WeightedRandomSelector;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Events extends AbstractProvider<BaseProviders> {
    private static final WeightedRandomSelector selector = new WeightedRandomSelector(new Random());

    public record Event(String name) {

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
        return EVENTS[faker.random().nextInt(EVENTS.length)];
    }

    public Event weightedEvent() {
        return selector.select(WEIGHTED_INSECTS);
    }
}