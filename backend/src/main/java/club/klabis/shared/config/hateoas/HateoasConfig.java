package club.klabis.shared.config.hateoas;

import club.klabis.shared.config.hateoas.forms.InputOptions;
import com.dpolach.spring.util.annotations.AnnotatedFieldScanner;
import com.dpolach.spring.util.annotations.AnnotatedFieldVisitor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HateoasConfiguration;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

@Profile("hateoas")
@Configuration
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL, EnableHypermediaSupport.HypermediaType.HAL_FORMS})
public class HateoasConfig {

    private static final Logger LOG = LoggerFactory.getLogger(HateoasConfig.class);

    @Bean
    HalFormsConfiguration kalConfiguration() {
        return addOptionsDefinitions(new HalFormsConfiguration()
                .withMediaType(KalMediaTypeConfiguration.KAL_MEDIA_TYPE)
                .withObjectMapperCustomizer(this::customizeHalFormsObjectMapper));
    }

    @Bean
    HalFormsConfiguration halFormsConfiguration() {
        return addOptionsDefinitions(new HalFormsConfiguration());
    }

    static HalFormsConfiguration addOptionsDefinitions(HalFormsConfiguration configuration) {
        for (Data d : options().toList()) {
            LOG.trace("Definiting options for %s#%s".formatted(d.type().getCanonicalName(), d.field.getName()));
            configuration = configuration.withOptions(d.type(),
                    d.field.getName(),
                    propertyMetadata -> HalFormsOptions.inline("H10", "D10", "H12", "D12"));
            // TODO: return serialized options from enum attribute
            // TODO: shall we automatically do that for all Enum types? (and leave InputOptions only for adding options to non-enum or customize default enum behavior?)
        }

        return configuration;
    }

    private void customizeHalFormsObjectMapper(ObjectMapper objectMapper) {
    }

    /**
     * Bean created in {@link HateoasConfiguration#messageResolver()} reads rest-messages using iso-8859-1 encoding. As we need czech characters, we have that resource in UTF-8. Let's overwrite that bean with decorator doing conversion of charset to UTF-8.
     *
     * @param messageResolver
     * @return
     */
    @Primary
    @Bean
    MessageResolver utf8MessageResolver(MessageResolver messageResolver) {
        return new MessageResolver() {

            static String convertToUtf8(String message) {
                if (message == null) {
                    return null;
                }
                return new String(message.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            }

            @Override
            public String resolve(MessageSourceResolvable resolvable) {
                return convertToUtf8(messageResolver.resolve(resolvable));
            }
        };
    }

    record Data(Field field, Class<?> type) {
    }

    static Stream<Data> options() {
        AnnotatedFieldScanner scanner = new AnnotatedFieldScanner();

        final Collection<Data> items = new ArrayList<>();

        AnnotatedFieldVisitor visitor = new AnnotatedFieldVisitor() {

            @Override
            public void visit(Field field, Class<?> enclosingClass) {
                items.add(new Data(field, enclosingClass));
            }

        };

        scanner.visitClassesWithAnnotatedFields("club.klabis", InputOptions.class, visitor);

        return items.stream();
    }
}
