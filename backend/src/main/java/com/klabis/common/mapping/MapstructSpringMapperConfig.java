package com.klabis.common.mapping;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * Shared MapStruct config for all Spring-managed mappers.
 * Generates a ConversionServiceAdapter bean so mappers can be resolved
 * via Spring's ConversionService instead of importing each other's
 * generated implementations with {@code uses = ...}.
 */
@MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
@SpringMapperConfig(
        conversionServiceAdapterPackage = "com.klabis.common.mapping",
        conversionServiceAdapterClassName = "KlabisConversionServiceAdapter"
)
public interface MapstructSpringMapperConfig {
}
