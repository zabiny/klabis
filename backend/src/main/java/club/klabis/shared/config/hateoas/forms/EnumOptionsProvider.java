package club.klabis.shared.config.hateoas.forms;

import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

import java.util.stream.Stream;

public class EnumOptionsProvider implements HalFormsOptionsProvider {
    private final Class<Enum<?>> enumClass;
    private final HalFormsOptions halFormsOptions;

    public EnumOptionsProvider(Class<Enum<?>> enumClass) {
        this.enumClass = enumClass;
        this.halFormsOptions = createOptions(enumClass);
    }

    static HalFormsOptions createOptions(Class<Enum<?>> enumClass) {
        try {
            Enum<?>[] enumConstants = enumClass.getEnumConstants();

            return HalFormsOptions.inline(Stream.of(enumConstants)
                    .map(Enum::name)
                    .toArray(String[]::new));
        } catch (ClassCastException e) {
            throw new IllegalStateException("Failed to process enum values for " + enumClass, e);
        }
    }

    @Override
    public HalFormsOptions createOptions(AffordanceModel.PropertyMetadata propertyMetadata) {
        return halFormsOptions;
    }
}
