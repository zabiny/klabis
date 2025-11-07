package org.springframework.hateoas;

import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

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
    public static Affordance affordImproved(Object invocationValue) {

        Affordance affordance = afford(invocationValue);

        Map<MediaType, AffordanceModel> models = new HashMap<>(affordance.getModels());

        Assert.state(models.containsKey(MediaTypes.HAL_FORMS_JSON), "Affordance model for HAL+FORMS is not present");
        AffordanceModel originalModel = models.get(MediaTypes.HAL_FORMS_JSON);

        AffordanceModel improvedModel = ImprovedHalFormsAffordanceModel.improveHalFormsAffordance(originalModel);
        models.put(MediaTypes.HAL_FORMS_JSON, improvedModel);

        return new Affordance(models);
    }

}
