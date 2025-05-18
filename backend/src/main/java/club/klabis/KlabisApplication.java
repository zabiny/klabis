package club.klabis;

import club.klabis.config.KlabisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KlabisProperties.class)
public class KlabisApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlabisApplication.class, args);
    }
}
