package club.klabis.shared.config.faker;

import net.datafaker.Faker;

public class KlabisFakerImpl extends Faker implements KlabisFaker {
    @Override
    public Clubs clubs() {
        return getProvider(Clubs.class, Clubs::new, this);
    }

    @Override
    public Events events() {
        return getProvider(Events.class, Events::new, this);
    }

    @Override
    public Cities cities() {
        return getProvider(Cities.class, Cities::new, this);
    }

}
