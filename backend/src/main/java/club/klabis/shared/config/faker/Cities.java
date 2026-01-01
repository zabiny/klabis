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
            "Brno", "Radostice", "Blansko", "Tisnov", "Znojmo", "Olomouc", "Ostrava", "Plzen",
            "Ceske Budejovice", "Karlovy Vary", "Liberec", "Usti nad Labem", "Jihlava", "Kolin"
    };
    private static final List<Map<String, Object>> WEIGHTED_CITIES = List.of(
            Map.of("value", CITIES[0], "weight", 5.0),   // Brno - primary
            Map.of("value", CITIES[1], "weight", 2.0),   // Radostice
            Map.of("value", CITIES[2], "weight", 2.0),   // Blansko
            Map.of("value", CITIES[3], "weight", 2.0),   // Tisnov
            Map.of("value", CITIES[4], "weight", 1.5),   // Znojmo
            Map.of("value", CITIES[5], "weight", 2.5),   // Olomouc
            Map.of("value", CITIES[6], "weight", 2.0),   // Ostrava
            Map.of("value", CITIES[7], "weight", 2.0),   // Plzen
            Map.of("value", CITIES[8], "weight", 1.5),   // Ceske Budejovice
            Map.of("value", CITIES[9], "weight", 1.0),   // Karlovy Vary
            Map.of("value", CITIES[10], "weight", 1.5),  // Liberec
            Map.of("value", CITIES[11], "weight", 1.0),  // Usti nad Labem
            Map.of("value", CITIES[12], "weight", 1.5),  // Jihlava
            Map.of("value", CITIES[13], "weight", 1.0)   // Kolin
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