package club.klabis.adapters.api.mappers;


import club.klabis.api.dto.RefereeLicenceApiDto;
import club.klabis.domain.members.RefereeLicence;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
interface RefereeLicenceApiDtoMapper extends Converter<RefereeLicence, RefereeLicenceApiDto> {

    @Override
    @Mapping(target = "licence", source = "licenceType")
    RefereeLicenceApiDto convert(RefereeLicence source);

    @DelegatingConverter
    @InheritInverseConfiguration
    RefereeLicence fromApiDto(RefereeLicenceApiDto apiDto);
}