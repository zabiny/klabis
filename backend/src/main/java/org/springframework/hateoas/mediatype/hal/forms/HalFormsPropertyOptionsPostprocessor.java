package org.springframework.hateoas.mediatype.hal.forms;

import org.springframework.hateoas.AffordanceModel;

import java.util.HashMap;
import java.util.Map;

class HalFormsPropertyOptionsPostprocessor implements HalFormsPropertyPostprocessor {

    private Map<String, HalFormsOptions> customOptions = new HashMap<>();

    public void defineOptions(String propertyName, HalFormsOptions options) {
        this.customOptions.put(propertyName, options);
    }

    @Override
    public HalFormsProperty postprocess(HalFormsProperty property, AffordanceModel.PropertyMetadata propertyMetadata) {
        if (customOptions.containsKey(property.getName())) {
            property = property.withOptions(customOptions.get(property.getName()));
        }

        if (property.getOptions() != null && property.getOptions()
                                                     .getMaxItems() != null && property.getOptions()
                                                                                       .getMaxItems() > 1) {
            property = property.withMulti(true);
        }
        return property;
    }
}
