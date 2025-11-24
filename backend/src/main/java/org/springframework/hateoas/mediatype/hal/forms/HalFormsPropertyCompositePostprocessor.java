package org.springframework.hateoas.mediatype.hal.forms;

import org.springframework.hateoas.AffordanceModel;

import java.util.List;
import java.util.Optional;

class HalFormsPropertyCompositePostprocessor implements HalFormsPropertyPostprocessor {

    private final List<HalFormsPropertyPostprocessor> postprocessors;

    static HalFormsPropertyCompositePostprocessor defaultSetup() {
        return new HalFormsPropertyCompositePostprocessor(
                List.of(
                        new HalFormsPropertyOptionsPostprocessor(),
                        new InputTypePostprocessor()
                )
        );
    }

    HalFormsPropertyCompositePostprocessor(List<HalFormsPropertyPostprocessor> postprocessors) {
        this.postprocessors = postprocessors;
    }

    public <T extends HalFormsPropertyPostprocessor> Optional<T> findPostprocessor(Class<T> type) {
        return postprocessors.stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    @Override
    public HalFormsProperty postprocess(HalFormsProperty property, AffordanceModel.PropertyMetadata propertyMetadata) {
        HalFormsProperty result = property;
        for (HalFormsPropertyPostprocessor postprocessor : this.postprocessors) {
            result = postprocessor.postprocess(result, propertyMetadata);
        }
        return result;
    }
}
