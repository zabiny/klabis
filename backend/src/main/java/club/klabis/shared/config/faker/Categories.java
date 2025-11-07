package club.klabis.shared.config.faker;

import club.klabis.events.domain.Competition;
import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.service.WeightedRandomSelector;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Categories extends AbstractProvider<BaseProviders> {

    private static final WeightedRandomSelector selector = new WeightedRandomSelector(new Random());

    private static final String[] STANDARD_NAMES = {
            "D16", "D18", "D21", "D35", "H16", "H18", "H21", "H35"
    };

    private static final String[] C_NAMES = {
            "D16C", "D18C", "D21C", "D35C", "H16C", "H18C", "H21C", "H35C"
    };

    private static final String[] TRAINING_NAMES = {
            "RED_XL", "RED_L", "RED_M", "ORANGE_XL", "ORANGE_L", "ORANGE_M"
    };


    private static final List<Map<String, Object>> WEIGHTED_CATEGORIES = List.of(
            Map.of("value", TRAINING_NAMES, "weight", 4.0),
            Map.of("value", C_NAMES, "weight", 4.0),
            Map.of("value", STANDARD_NAMES, "weight", 2.0)
    );

    private static final String[][] SETS = {STANDARD_NAMES, C_NAMES, TRAINING_NAMES};

    public Categories(BaseProviders faker) {
        super(faker);
    }

    public Set<Competition.Category> randomCategories() {
        return toCategories(SETS[faker.random().nextInt(SETS.length)]);
    }

    static Set<Competition.Category> toCategories(String[] names) {
        return Stream.of(names).map(c -> new Competition.Category(c)).collect(Collectors.toSet());
    }

    public Set<Competition.Category> weightedCategories() {
        return toCategories(selector.select(WEIGHTED_CATEGORIES));
    }
}
