package club.klabis.members.adapters.restapi.mappers;

import club.klabis.api.dto.SexApiDto;
import club.klabis.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.Sex;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface SexApiDtoMapper extends Converter<Sex, SexApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    Sex fromApiDto(SexApiDto apiDto);
}
