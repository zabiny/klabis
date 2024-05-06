package club.klabis;

import org.springframework.boot.SpringApplication;

public class KlabisAppWithTestContainers {
    public static void main(String[] args) {
        SpringApplication.from(KlabisApplication::main)
                .with(ContainersConfiguration.class)
                .run(args);
    }
}
