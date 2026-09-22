package com.klabis.common.bootstrap;

import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * design.md D14: the four example-data initialisers still bootstrap when {@code oris} is inactive.
 */
@SpringBootTest
@ActiveProfiles({"test", "example-data"})
@Import(TestApplicationConfiguration.class)
@DisplayName("example-data active alone")
class SampleDataOnlyExampleDataActiveTest {

    @Autowired
    ApplicationContext context;

    @Test
    void sampleDataInitializersArePresent() {
        Set<String> foundSimpleNames = new HashSet<>();
        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type != null && SampleDataBootstrapNames.matchesAny(type)) {
                foundSimpleNames.add(type.getSimpleName());
            }
        }

        assertThat(foundSimpleNames).containsExactlyInAnyOrder(SampleDataBootstrapNames.ALL);
    }
}
