package club.klabis.oris.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oris-integration")
public record OrisProperties(boolean enabled) {


}
