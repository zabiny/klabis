package club.klabis.members.adapters.restapi.mappers;


import club.klabis.api.dto.TrainerLicenceApiDto;
import club.klabis.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.TrainerLicence;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface TrainerLicenceApiDtoMapper extends Converter<TrainerLicence, TrainerLicenceApiDto> {
    @Override
    @Mapping(target = "licence", source = "type")
    TrainerLicenceApiDto convert(TrainerLicence source);

    @DelegatingConverter
    @InheritInverseConfiguration
    TrainerLicence fromApiDto(TrainerLicenceApiDto apiDto);
}