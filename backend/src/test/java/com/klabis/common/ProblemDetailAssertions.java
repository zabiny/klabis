package com.klabis.common;

import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code detail} and {@code instance} are developer-facing and may carry identifiers; everything else in a
 * problem detail is shown to the user and must not.
 */
public final class ProblemDetailAssertions {

    private static final Pattern UUID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Set<String> DEVELOPER_FACING = Set.of("detail", "instance");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProblemDetailAssertions() {
    }

    public static ResultMatcher noIdentifiersInParameters() {
        return result -> {
            JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
            body.properties().stream()
                    .filter(property -> !DEVELOPER_FACING.contains(property.getKey()))
                    .forEach(property -> assertThat(UUID.matcher(property.getValue().toString()).find())
                            .as("problem detail property '%s' must not contain an identifier", property.getKey())
                            .isFalse());
        };
    }
}
