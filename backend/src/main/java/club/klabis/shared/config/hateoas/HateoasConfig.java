package club.klabis.shared.config.hateoas;

import club.klabis.shared.config.hateoas.forms.OptionsProviderFactory;
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

import java.nio.charset.StandardCharsets;

@Profile("hateoas")
@Configuration
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL, EnableHypermediaSupport.HypermediaType.HAL_FORMS})
public class HateoasConfig {

    private static final Logger LOG = LoggerFactory.getLogger(HateoasConfig.class);

    @Bean
    HalFormsConfiguration kalConfiguration() {
        return OptionsProviderFactory.addOptionsDefinitions(new HalFormsConfiguration()
                .withMediaType(KalMediaTypeConfiguration.KAL_MEDIA_TYPE)
                .withObjectMapperCustomizer(this::customizeHalFormsObjectMapper));
    }

    @Bean
    HalFormsConfiguration halFormsConfiguration() {
        return OptionsProviderFactory.addOptionsDefinitions(new HalFormsConfiguration());
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


}