package com.klabis.sync;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.infrastructure.restapi.EntityDescription;
import com.klabis.sync.infrastructure.restapi.SyncEntityDescriber;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;

import java.util.Optional;

@TestConfiguration
public class SyncDescriberStubConfiguration {

    @Bean
    SyncEntityDescriber stubEventDescriber() {
        return new SyncEntityDescriber() {
            @Override
            public SyncEntityType entityType() {
                return SyncEntityType.EVENT;
            }

            @Override
            public Optional<EntityDescription> describe(String entityId) {
                if ("event-1".equals(entityId)) {
                    return Optional.of(new EntityDescription("Spring Cup", Link.of("http://localhost/api/events/event-1")));
                }
                if ("event-boom".equals(entityId)) {
                    throw new IllegalStateException("lookup failed");
                }
                if ("event-nolink".equals(entityId)) {
                    return Optional.of(new EntityDescription("Hidden Cup", null));
                }
                return Optional.empty();
            }
        };
    }
}
