package club.klabis.members.infrastructure.restapi.mappers;


import club.klabis.members.domain.RefereeLicence;
import club.klabis.members.infrastructure.restapi.dto.RefereeLicenceApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface RefereeLicenceApiDtoMapper extends Converter<RefereeLicence, RefereeLicenceApiDto> {

    @Override
    @Mapping(target = "licence", source = "licenceType")
    RefereeLicenceApiDto convert(RefereeLicence source);

    @DelegatingConverter
    @InheritInverseConfiguration
    RefereeLicence fromApiDto(RefereeLicenceApiDto apiDto);
}