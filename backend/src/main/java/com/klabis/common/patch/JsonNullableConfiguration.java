package com.klabis.common.patch;

import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the JsonNullable Jackson 3 module.
 *
 * <p>Sibling modules use {@code @JacksonComponent}, which is not an option here: the module class
 * belongs to the library and cannot be annotated. The Jackson 2 variant shipped in the same artifact
 * must not be registered — Spring Boot 4 builds a Jackson 3 {@code ObjectMapper}.
 */
@Configuration(proxyBeanMethods = false)
public class JsonNullableConfiguration {

    @Bean
    JsonNullableJackson3Module jsonNullableJackson3Module() {
        return new JsonNullableJackson3Module();
    }
}
