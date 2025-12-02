package club.klabis.shared.config.hateoas.forms;

import com.dpolach.spring.util.annotations.AnnotatedFieldScanner;
import com.dpolach.spring.util.annotations.AnnotatedFieldVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public class OptionsProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OptionsProviderFactory.class);

    static HalFormsOptionsProvider getProvider(Class<?> type, Field property, InputOptions annotation) {
        if (annotation.sourceEnum() != Object.class && Enum.class.isAssignableFrom(annotation.sourceEnum())) {
            return new EnumOptionsProvider((Class<Enum<?>>) annotation.sourceEnum());
        } else if (Enum.class.isAssignableFrom(property.getType())) {
            return new EnumOptionsProvider((Class<Enum<?>>) property.getType());
        } else {
            throw new RuntimeException("Incorrect InputOptions property %s#%s".formatted(type.getName(),
                    property.getName()));
        }
    }

    record HalFormsOptionsDescriptor(Field field, Class<?> type, HalFormsOptionsProvider provider) {
        public String describe() {
            return "Options for %s#%s with provider by %s".formatted(type.getSimpleName(),
                    field.getName(),
                    provider.getClass().getSimpleName()).formatted();
        }
    }

    public static HalFormsConfiguration addOptionsDefinitions(HalFormsConfiguration configuration) {
        for (OptionsProviderFactory.HalFormsOptionsDescriptor d : OptionsProviderFactory.optionsProviders().toList()) {
            LOG.trace(d.describe());
            configuration = configuration.withOptions(d.type(), d.field().getName(), d.provider()::createOptions);
            // TODO: shall we automatically do that for all Enum types? (and leave InputOptions only for adding options to non-enum or customize default enum behavior?)
        }

        return configuration;
    }

    static Stream<HalFormsOptionsDescriptor> optionsProviders() {
        AnnotatedFieldScanner scanner = new AnnotatedFieldScanner();

        final Collection<HalFormsOptionsDescriptor> items = new ArrayList<>();

        AnnotatedFieldVisitor visitor = new AnnotatedFieldVisitor() {

            @Override
            public void visit(Field field, Class<?> enclosingClass) {
                InputOptions annotation = field.getAnnotation(InputOptions.class);
                items.add(new HalFormsOptionsDescriptor(field,
                        enclosingClass,
                        getProvider(enclosingClass, field, annotation)));
            }

        };

        scanner.visitClassesWithAnnotatedFields("club.klabis", InputOptions.class, visitor);

        return items.stream();
    }


}
