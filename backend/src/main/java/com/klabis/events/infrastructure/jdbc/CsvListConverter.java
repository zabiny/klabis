package com.klabis.events.infrastructure.jdbc;

import java.util.Arrays;
import java.util.List;

/**
 * Shared utility for converting between comma-separated strings and List<String> in mementos.
 */
class CsvListConverter {

    private CsvListConverter() {
    }

    static String serialize(List<String> list) {
        return (list == null || list.isEmpty()) ? null : String.join(",", list);
    }

    static List<String> deserialize(String csv) {
        return (csv != null && !csv.isBlank()) ? Arrays.asList(csv.split(",")) : List.of();
    }
}
