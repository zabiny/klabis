package club.klabis.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klabis")
public record KlabisProperties(boolean presetData) {
}
