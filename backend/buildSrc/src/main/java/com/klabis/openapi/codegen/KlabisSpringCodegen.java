package com.klabis.openapi.codegen;

import org.openapitools.codegen.languages.SpringCodegen;

/**
 * Klabis-specific fork of the stock {@code spring} OpenAPI generator.
 *
 * <p>Vendor fork, diff on upgrade: this class overrides specific protected extension points of
 * {@link SpringCodegen} to teach the generator Klabis's own HAL conventions (envelope schemas,
 * tag-scoped model discovery) natively, instead of compensating for their absence with Gradle-side
 * whitelists, {@code schemaMappings}, and post-process patches. See
 * {@code openspec/changes/custom-openapi-codegen/design.md} for the full rationale and the
 * shape-detection rules this class implements.
 */
public class KlabisSpringCodegen extends SpringCodegen {

    @Override
    public String getName() {
        return "klabis-spring";
    }
}
