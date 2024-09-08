package club.klabis.common.mapstruct;

import club.klabis.common.ConversionServiceAdapter;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Mapstruct configuration for API DTO mappers.
 * These mappers shall implement `Converter<DOMAIN, APIDTO>` (APIDTO needs to be target, DOMAIN object needs to be source)
 */
@MapperConfig(componentModel = "spring", uses = {ConversionServiceAdapter.class, OptionalMapstructSupport.class}, unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DomainToDtoMapperConfiguration {
}
