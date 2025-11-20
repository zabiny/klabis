package club.klabis.shared.config.hateoas.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.util.function.ThrowingFunction;

import java.util.stream.Stream;

class EnumOptionsProvider implements HalFormsOptionsProvider {
    private final HalFormsOptions halFormsOptions;

    public EnumOptionsProvider(Class<Enum<?>> enumClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        this.halFormsOptions = createOptions(enumClass, objectMapper::writeValueAsString);
    }

    static HalFormsOptions createOptions(Class<Enum<?>> enumClass, ThrowingFunction<Enum<?>, String> toValue) {
        try {
            Enum<?>[] enumConstants = enumClass.getEnumConstants();

            return HalFormsOptions.inline(Stream.of(enumConstants)
                    .map(toValue)
                    .map(v -> v.replaceAll("\"", ""))
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
