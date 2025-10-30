package club.klabis.shared.config.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.service.WeightedRandomSelector;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Clubs extends AbstractProvider<BaseProviders> {
    private static final WeightedRandomSelector selector = new WeightedRandomSelector(new Random());

    public record Club(String name, String code) {

    }

    private static final Club[] CLUBS = {new Club("SK Brno Zabovresky", "ZBM"), new Club("Tesla Brno", "TBM"), new Club(
            "Beta Ursus",
            "BBM")};
    private static final List<Map<String, Object>> WEIGHTED_INSECTS = List.of(
            Map.of("value", CLUBS[0], "weight", 6.0),
            Map.of("value", CLUBS[1], "weight", 3.0),
            Map.of("value", CLUBS[2], "weight", 1.0)
    );

    public Clubs(BaseProviders faker) {
        super(faker);
    }

    public Club nextClub() {
        return CLUBS[faker.random().nextInt(CLUBS.length)];
    }

    public Club weightedClub() {
        return selector.select(WEIGHTED_INSECTS);
    }
}