package com.klabis.events.infrastructure.restapi;

import java.text.Normalizer;

class EventNameSlugifier {

    private EventNameSlugifier() {}

    static String slugify(String name) {
        if (name == null || name.isBlank()) {
            return "event";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
