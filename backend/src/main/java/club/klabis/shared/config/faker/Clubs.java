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

    private static final Club[] CLUBS = {
            new Club("SK Brno Zabovresky", "ZBM"),
            new Club("Tesla Brno", "TBM"),
            new Club("Beta Ursus", "BBM"),
            new Club("Badminton Brno", "BBr"),
            new Club("SK Olomouc", "SKO"),
            new Club("Ostrava Badminton Club", "OBC"),
            new Club("Plzen Rackets", "PRK"),
            new Club("Jihlava Shuttlers", "JHS"),
            new Club("Liberec Badminton", "LBD"),
            new Club("Ceske Budejovice BC", "CBC"),
            new Club("Karlovy Vary Sports Club", "KVSC"),
            new Club("Usti Badminton", "USB")
    };
    private static final List<Map<String, Object>> WEIGHTED_CLUBS = List.of(
            Map.of("value", CLUBS[0], "weight", 6.0),   // SK Brno Zabovresky
            Map.of("value", CLUBS[1], "weight", 3.0),   // Tesla Brno
            Map.of("value", CLUBS[2], "weight", 1.0),   // Beta Ursus
            Map.of("value", CLUBS[3], "weight", 2.5),   // Badminton Brno
            Map.of("value", CLUBS[4], "weight", 2.0),   // SK Olomouc
            Map.of("value", CLUBS[5], "weight", 1.5),   // Ostrava Badminton Club
            Map.of("value", CLUBS[6], "weight", 1.5),   // Plzen Rackets
            Map.of("value", CLUBS[7], "weight", 1.0),   // Jihlava Shuttlers
            Map.of("value", CLUBS[8], "weight", 1.5),   // Liberec Badminton
            Map.of("value", CLUBS[9], "weight", 1.0),   // Ceske Budejovice BC
            Map.of("value", CLUBS[10], "weight", 0.8),  // Karlovy Vary Sports Club
            Map.of("value", CLUBS[11], "weight", 0.8)   // Usti Badminton
    );

    public Clubs(BaseProviders faker) {
        super(faker);
    }

    public Club nextClub() {
        return CLUBS[faker.random().nextInt(CLUBS.length)];
    }

    public Club weightedClub() {
        return selector.select(WEIGHTED_CLUBS);
    }
}