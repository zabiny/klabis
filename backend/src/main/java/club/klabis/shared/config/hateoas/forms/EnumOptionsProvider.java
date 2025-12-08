package club.klabis.shared.config.hateoas.forms;

import club.klabis.shared.config.hateoas.HalFormsOptionItem;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

import java.util.Collection;
import java.util.stream.Stream;

class EnumOptionsProvider<T extends Enum<T>> implements HalFormsOptionsProvider {
    private final HalFormsOptions halFormsOptions;

    public EnumOptionsProvider(Class<T> enumClass) {
        this.halFormsOptions = createOptions(enumClass);
    }

    HalFormsOptionItem<T> createOption(T value) {
        return new HalFormsOptionItem<>(value, value.name());
    }

    HalFormsOptions createOptions(Class<T> enumClass) {
        try {
            Collection<HalFormsOptionItem<T>> items = Stream.of(enumClass.getEnumConstants())
                    .map(this::createOption)
                    .toList();

            return HalFormsOptions.inline(items);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Failed to process enum values for " + enumClass, e);
        }
    }

    @Override
    public HalFormsOptions createOptions(AffordanceModel.PropertyMetadata propertyMetadata) {
        return halFormsOptions;
    }
}
