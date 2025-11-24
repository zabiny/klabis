package org.springframework.hateoas.mediatype.hal.forms;

import club.klabis.shared.config.hateoas.KlabisInputTypes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.AffordanceModel;

class InputTypePostprocessor implements HalFormsPropertyPostprocessor {

    @Override
    public HalFormsProperty postprocess(HalFormsProperty property, AffordanceModel.PropertyMetadata propertyMetadata) {

        String inputType = propertyMetadata.getInputType();

        if (StringUtils.isBlank(inputType)) {
            Class<?> dataType = propertyMetadata.getType().getRawClass();

            if (dataType != null) {
                if (Enum.class.isAssignableFrom(dataType)) {
                    return property.withType(KlabisInputTypes.RADIO_INPUT_TYPE);
                } else if (Boolean.class.isAssignableFrom(dataType) || boolean.class.isAssignableFrom(dataType)) {
                    return property.withType(KlabisInputTypes.BOOLEAN_INPUT_TYPE);
                }

                return property.withType(dataType.getSimpleName());
            }
        }

        return property;
    }
}
