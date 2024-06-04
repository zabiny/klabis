package club.klabis;

import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

@Configuration
public class ContainersConfiguration {

    @Bean
    @ServiceConnection
    @RestartScope
    PostgreSQLContainer<?> createPostgreSQLDBContainer() {
        return new PostgreSQLContainer<>("postgres")
                .withEnv(Map.of("PGDATA", "/var/lib/postgresql/data"))
                .withTmpFs(Map.of("/var/lib/postgresql/data", "rw"));
    }

}
