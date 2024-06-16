package club.klabis.adapters.api.mappers;


import club.klabis.api.dto.OBLicenceApiDto;
import club.klabis.domain.members.OBLicence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
interface OBLicenceApiDtoMapper extends Converter<OBLicence, OBLicenceApiDto> {
    @Override
    @Mapping(target = "licence", source = ".")
    OBLicenceApiDto convert(OBLicence source);

//    @DelegatingConverter
//    @InheritInverseConfiguration
//    OBLicence fromApiDto(OBLicenceApiDto apiDto);
}