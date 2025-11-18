package club.klabis.shared.config.hateoas.forms;

import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

interface HalFormsOptionsProvider {
    HalFormsOptions createOptions(AffordanceModel.PropertyMetadata propertyMetadata);
}
