package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.LegalGuardianApiDto;
import club.klabis.domain.members.LegalGuardian;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
interface LegalGuardianApiDtoMapper extends Converter<LegalGuardian, LegalGuardianApiDto> {

    @Override
    @Mapping(source = "contacts", target = "contact")
    LegalGuardianApiDto convert(LegalGuardian source);

    @DelegatingConverter
    @InheritInverseConfiguration
    LegalGuardian fromApiDto(LegalGuardianApiDto apiDto);

}
