package club.klabis.shared.config.hateoas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;

@Profile("hateoas")
@Configuration
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL, EnableHypermediaSupport.HypermediaType.HAL_FORMS})
class Config {

    @Bean
    HalFormsConfiguration halFormsConfiguration() {
        return new HalFormsConfiguration()
                .withMediaType(KalMediaTypeConfiguration.KAL_MEDIA_TYPE)
                .withObjectMapperCustomizer(this::customizeHalFormsObjectMapper);
    }

    private void customizeHalFormsObjectMapper(ObjectMapper objectMapper) {
    }
}
