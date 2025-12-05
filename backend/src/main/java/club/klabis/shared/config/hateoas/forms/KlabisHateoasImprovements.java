package club.klabis.shared.config.hateoas.forms;

import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;

import java.util.function.Consumer;

public class KlabisHateoasImprovements {

    public static Affordance affordBetter(@NonNull Object invocation) {
        return org.springframework.hateoas.KlabisHateoasImprovements.affordImproved(invocation);
    }

    public static Affordance affordBetter(@NonNull Object invocation, @NonNull Consumer<ImprovedHalFormsAffordanceModel> postprocessor) {
        return org.springframework.hateoas.KlabisHateoasImprovements.affordImproved(invocation, postprocessor);
    }

}
