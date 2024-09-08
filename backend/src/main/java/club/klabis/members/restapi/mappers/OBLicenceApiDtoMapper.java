package club.klabis.members.restapi.mappers;


import club.klabis.api.dto.OBLicenceApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.OBLicence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface OBLicenceApiDtoMapper extends Converter<OBLicence, OBLicenceApiDto> {
    @Override
    @Mapping(target = "licence", source = ".")
    OBLicenceApiDto convert(OBLicence source);

//    @DelegatingConverter
//    @InheritInverseConfiguration
//    OBLicence fromApiDto(OBLicenceApiDto apiDto);
}