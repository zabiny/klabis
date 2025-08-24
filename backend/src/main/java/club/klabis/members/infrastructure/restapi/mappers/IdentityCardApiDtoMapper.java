package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.IdentityCard;
import club.klabis.members.infrastructure.restapi.dto.IdentityCardApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class, unmappedSourcePolicy = ReportingPolicy.WARN)
interface IdentityCardApiDtoMapper extends Converter<IdentityCard, IdentityCardApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    IdentityCard fromApiDto(IdentityCardApiDto apiDto);
}
