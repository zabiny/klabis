package club.klabis;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KlabisApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlabisApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {

            System.out.println("Spring funguje... :)");

        };
    }
}
