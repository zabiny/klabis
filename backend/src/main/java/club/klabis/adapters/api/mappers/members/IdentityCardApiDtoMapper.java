package club.klabis.adapters.api.mappers.members;

import club.klabis.api.dto.IdentityCardApiDto;
import club.klabis.common.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.IdentityCard;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class, unmappedSourcePolicy = ReportingPolicy.ERROR)
interface IdentityCardApiDtoMapper extends Converter<IdentityCard, IdentityCardApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    IdentityCard fromApiDto(IdentityCardApiDto apiDto);
}
