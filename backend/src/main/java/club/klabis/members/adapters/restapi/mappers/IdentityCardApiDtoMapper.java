package club.klabis.members.adapters.restapi.mappers;

import club.klabis.api.dto.IdentityCardApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.IdentityCard;
import org.mapstruct.*;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class, unmappedSourcePolicy = ReportingPolicy.WARN)
interface IdentityCardApiDtoMapper extends Converter<IdentityCard, IdentityCardApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    IdentityCard fromApiDto(IdentityCardApiDto apiDto);
}
