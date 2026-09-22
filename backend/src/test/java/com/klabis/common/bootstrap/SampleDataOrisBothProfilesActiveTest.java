package com.klabis.common.bootstrap;

import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * design.md D14: the four example-data initialisers stand down when {@code oris} is active,
 * so ORIS-sourced members never collide with sample registration numbers.
 */
@SpringBootTest
@ActiveProfiles({"test", "example-data", "oris"})
@Import(TestApplicationConfiguration.class)
@DisplayName("example-data and oris active together")
class SampleDataOrisBothProfilesActiveTest {

    @Autowired
    ApplicationContext context;

    @Test
    void sampleDataInitializersAreAbsent() {
        assertThat(context.getBeanDefinitionNames())
                .noneMatch(name -> SampleDataBootstrapNames.matchesAny(context.getType(name)));
    }
}
