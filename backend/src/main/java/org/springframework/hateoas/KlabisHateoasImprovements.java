package org.springframework.hateoas;

import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;

public class KlabisHateoasImprovements {

    /**
     * Default {@link WebMvcLinkBuilder#afford(Object)} creates affordance where properties in _templates are ordered by their name.
     * For KlabisForm we would like to keep that order - and that's what this alternative can do.
     * <p>
     * Also some additional improvements are done (Options, types, etc.. )
     *
     * @param invocationValue
     * @return
     */
    public static List<Affordance> affordImproved(Object invocationValue) {
        return affordImproved(invocationValue, NO_ACTION);
    }

    private static final Consumer<Object> NO_ACTION = a -> {
    };

    public static List<Affordance> affordImproved(Object invocationValue, Consumer<? super ImprovedHalFormsAffordanceModel> postprocess) {
        Assert.isInstanceOf(LastInvocationAware.class, invocationValue);

        LastInvocationAware invocation = DummyInvocationUtils.getLastInvocationAware(invocationValue);
        if (!isAuthorizedToCall(invocation)) {
            return List.of();
        }

        Affordance affordance = afford(invocationValue);

        Map<MediaType, AffordanceModel> models = new HashMap<>(affordance.getModels());

        Assert.state(models.containsKey(MediaTypes.HAL_FORMS_JSON), "Affordance model for HAL+FORMS is not present");
        AffordanceModel originalModel = models.get(MediaTypes.HAL_FORMS_JSON);

        ImprovedHalFormsAffordanceModel improvedModel = ImprovedHalFormsAffordanceModel.improveHalFormsAffordance(
                originalModel);
        postprocess.accept(improvedModel);
        models.put(MediaTypes.HAL_FORMS_JSON, improvedModel);

        return List.of(new Affordance(models));
    }

    public static boolean isAuthorizedToCall(LastInvocationAware invocation) {
        if (invocation == null || invocation.getLastInvocation() == null) {
            return false;
        }

        return SecurityUtils.isAuthorizedToCall(invocation.getLastInvocation());
    }

}
