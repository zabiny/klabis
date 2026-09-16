package com.klabis.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OrisIntegrationComponentTest {

    @OrisIntegrationComponent
    @Configuration
    static class OrisGatedBean {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OrisGatedBean.class);

    @Test
    void beanRegistersWhenOrisProfileActive() {
        contextRunner.withPropertyValues("spring.profiles.active=oris")
                .run(context -> assertThat(context).hasSingleBean(OrisGatedBean.class));
    }

    @Test
    void beanDoesNotRegisterWithoutOrisProfile() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OrisGatedBean.class));
    }
}
