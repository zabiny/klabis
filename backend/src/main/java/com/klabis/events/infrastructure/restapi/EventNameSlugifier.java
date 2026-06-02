package com.klabis.events.infrastructure.restapi;

import com.klabis.common.jdbc.UnaccentFunction;

class EventNameSlugifier {

    private EventNameSlugifier() {}

    static String slugify(String name) {
        if (name == null || name.isBlank()) {
            return "event";
        }
        return UnaccentFunction.unaccent(name)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
