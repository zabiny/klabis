package com.klabis.sync.infrastructure.restapi;

import java.util.Optional;

record Described<V>(V view, Optional<EntityDescription> description) {

    String labelOrElse(String externalId) {
        return description.map(EntityDescription::label).orElse(externalId);
    }
}
