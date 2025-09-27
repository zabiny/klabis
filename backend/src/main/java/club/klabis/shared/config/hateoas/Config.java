package club.klabis.shared.config.hateoas;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@Profile("hateoas")
@Configuration
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL, EnableHypermediaSupport.HypermediaType.HAL_FORMS})
class Config {
}
