package club.klabis;

import club.klabis.shared.config.KlabisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@EnableConfigurationProperties(KlabisProperties.class)
@Modulithic
public class KlabisApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlabisApplication.class, args);
    }
}
