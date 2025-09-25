package club.klabis.oris.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrisProperties.class)
class OrisConfiguration {
}
