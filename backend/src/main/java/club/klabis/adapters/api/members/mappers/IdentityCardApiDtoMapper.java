package club.klabis.adapters.api.members.mappers;

import club.klabis.api.dto.IdentityCardApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.IdentityCard;
import org.mapstruct.*;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class, unmappedSourcePolicy = ReportingPolicy.WARN)
interface IdentityCardApiDtoMapper extends Converter<IdentityCard, IdentityCardApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    IdentityCard fromApiDto(IdentityCardApiDto apiDto);
}
