package club.klabis.shared.config.hateoas.forms;

import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class KlabisHateoasImprovements {

    public static List<Affordance> affordBetter(@NonNull Object invocation) {
        return org.springframework.hateoas.KlabisHateoasImprovements.affordImproved(invocation);
    }

    public static List<Affordance> affordBetter(@NonNull Object invocation, @NonNull Consumer<ImprovedHalFormsAffordanceModel> postprocessor) {
        return org.springframework.hateoas.KlabisHateoasImprovements.affordImproved(invocation, postprocessor);
    }

    /**
     * Method is usable during preparation HATEOAS links and affordances. It "filters" out invocation if method used to create it is not allowed for authenticated user.
     *
     * @param invocation invocation object, usually retrieved using @{@link org.springframework.hateoas.server.mvc.WebMvcLinkBuilder#methodOn(Class, Object...)} factory.
     * @return Empty optional if currently logged in user is not authorized to call that method. Optional with same invocation if such user is authorized to call that method.
     */
    public static Optional<WebMvcLinkBuilder> linkIfAuthorized(Object invocation) {
        return org.springframework.hateoas.KlabisHateoasImprovements.invocationIfAuthorized(invocation)
                .map(WebMvcLinkBuilder::linkTo);
    }
}
