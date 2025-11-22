package org.springframework.hateoas.mediatype.hal.forms;

import org.springframework.hateoas.AffordanceModel;

public interface HalFormsPropertyPostprocessor {
    HalFormsProperty postprocess(HalFormsProperty property, AffordanceModel.PropertyMetadata propertyMetadata);
}
