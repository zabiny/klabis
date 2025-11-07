package club.klabis.shared.config.hateoas.forms;

import org.springframework.hateoas.Affordance;

public class KlabisHateoasImprovements {

    public static Affordance affordBetter(Object invocation) {
        return org.springframework.hateoas.KlabisHateoasImprovements.affordImproved(invocation);
    }

}
