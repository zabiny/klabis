package com.klabis.common.mapping;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * Shared MapStruct config for all Spring-managed mappers: componentModel = "spring" with
 * constructor injection. The generated cross-mapper adapter is emitted into
 * {@code com.klabis.mapstruct} — a package outside every Spring Modulith module — so the classes it
 * references (including module-private {@code .domain} types) are not seen by
 * {@code ModuleStructureVerificationTest} as illegal cross-module dependencies.
 */
@MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
@SpringMapperConfig(
        conversionServiceAdapterPackage = "com.klabis.mapstruct",
        conversionServiceAdapterClassName = "KlabisConversionServiceAdapter"
)
public interface MapstructSpringMapperConfig {
}
