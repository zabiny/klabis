package com.klabis.common.mapping;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;

/**
 * Shared MapStruct config for all Spring-managed mappers: componentModel = "spring" with
 * constructor injection. Does not generate a cross-mapper ConversionServiceAdapter — no mapper in
 * this codebase composes another mapper's output via {@code uses = <OtherMapper>}, and generating
 * that adapter is actively harmful: it aggregates every Converter's source/target types into one
 * class in {@code com.klabis.common.mapping}, so as soon as any Converter touches a
 * Modulith-module-private {@code .domain} type (e.g. {@code members.domain.Member}), the generated
 * adapter makes the {@code common} module illegally depend on that module's internals, failing
 * {@code ModuleStructureVerificationTest}.
 */
@MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface MapstructSpringMapperConfig {
}
