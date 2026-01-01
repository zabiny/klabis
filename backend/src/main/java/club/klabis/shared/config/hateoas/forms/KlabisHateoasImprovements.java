package club.klabis.shared.config.hateoas.forms;

import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.LinkWithEmbeddedResource;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.util.Assert;

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
     * Can be used to link another resource together with embedding that resource in HAL response.
     */
    // TODO: finish serialization of embeddable data from link into _embedded list in HalResponse.
    public static Optional<Link> linkWithEmbeddedResponse(LinkRelation rel, @NonNull Object invocation, @NonNull Object invocationTarget) {
        Assert.isTrue(invocation instanceof LastInvocationAware,
                "invocation must be LastInvocationAware (was methodOn() used to produce that value?)");
        LastInvocationAware lastInvocationAware = DummyInvocationUtils.getLastInvocationAware(invocation);
        return linkIfAuthorized(invocation)
                .map(link -> new LinkWithEmbeddedResource(link.withRel(rel), lastInvocationAware, invocationTarget));
    }

    /**
     * Shall be used to link another resource together with embedding that resource in HAL response.
     */
    public static <T> Optional<Link> linkWithEmbeddedResponse(LinkRelation rel, @NonNull T invocationTarget, Consumer<T> invocationTargetProxy) {
        Object invocation = WebMvcLinkBuilder.methodOn(invocationTarget.getClass());
        invocationTargetProxy.accept((T) invocation);
        return linkWithEmbeddedResponse(rel, invocation, invocationTarget);
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
