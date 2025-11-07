package club.klabis.shared.config.hateoas.forms;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.util.Assert;

import java.util.List;
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
    public static Affordance affordBetter(Object invocationValue) {

        Affordance affordance = afford(invocationValue);

        AffordanceModel halFormsAffordanceModel = affordance.getAffordanceModel(MediaTypes.HAL_FORMS_JSON);

        Assert.state(halFormsAffordanceModel != null, "Affordance model should not be null");

        return new Affordance(Map.of(MediaTypes.HAL_FORMS_JSON, improveHalFormsAffordance(halFormsAffordanceModel)));
    }

    static AffordanceModel improveHalFormsAffordance(AffordanceModel original) {
        Class<?> requestBodyType = getRequestBodyType(original.getInput());

        List<String> expectedPropertiesOrder = expectedPropertiesOrder(requestBodyType);

        return new ImprovedHalFormsAffordanceModel(ImprovedHalFormsAffordanceModel.fromModel(original,
                expectedPropertiesOrder));
    }

    static final SerializationConfig objectMapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
            .registerModule(new ParameterNamesModule())
            .getSerializationConfig();

    static List<String> expectedPropertiesOrder(Class<?> bodyType) {
        return objectMapper.introspect(TypeFactory.defaultInstance().constructType(bodyType))
                .findProperties()
                .stream()
                .map(BeanPropertyDefinition::getName)
                .toList();
    }

    static Class<?> getRequestBodyType(AffordanceModel.InputPayloadMetadata metadata) {
        Assert.notNull(metadata.getType(),
                "Input payload type is null (need to read it from method paramter annotated with RequestBody)");
        return metadata.getType();
    }

}
