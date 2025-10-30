package club.klabis.shared.config.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.service.WeightedRandomSelector;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Cities extends AbstractProvider<BaseProviders> {
    private static final WeightedRandomSelector selector = new WeightedRandomSelector(new Random());

    private static final String[] CITIES = {
            "Brno", "Radostice", "Blansko", "Tisnov", "Znojmo"
    };
    private static final List<Map<String, Object>> WEIGHTED_CITIES = List.of(
            Map.of("value", CITIES[0], "weight", 4.0),
            Map.of("value", CITIES[1], "weight", 2.0),
            Map.of("value", CITIES[2], "weight", 2.0),
            Map.of("value", CITIES[3], "weight", 2.0)
    );

    public Cities(BaseProviders faker) {
        super(faker);
    }

    public String nextCity() {
        return CITIES[faker.random().nextInt(CITIES.length)];
    }

    public String weightedCity() {
        return selector.select(WEIGHTED_CITIES);
    }
}