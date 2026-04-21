package com.klabis.common.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BootstrapDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataLoader.class);

    private final List<BootstrapDataInitializer> bootstrapDataInitializers;

    public BootstrapDataLoader(List<BootstrapDataInitializer> bootstrapDataInitializers) {
        this.bootstrapDataInitializers = bootstrapDataInitializers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapDataInitializers.forEach(this::bootstrapData);
    }

    private void bootstrapData(BootstrapDataInitializer bootstrapDataInitializer) {
        try {
            if (bootstrapDataInitializer.requiresBootstrap()) {
                log.info("Running bootstrap data initializer {}", bootstrapDataInitializer.getClass().getSimpleName());
                bootstrapDataInitializer.bootstrapData();
            } else {
                log.trace("Bootstrap data from initializer {} are completed, skipping",
                        bootstrapDataInitializer.getClass().getSimpleName());
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize bootstrap data using %s".formatted(bootstrapDataInitializer.getClass()
                    .getCanonicalName()), ex);
        }
    }
}
