package club.klabis.adapters.api.members.mappers;

import club.klabis.api.dto.LegalGuardianApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.LegalGuardian;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface LegalGuardianApiDtoMapper extends Converter<LegalGuardian, LegalGuardianApiDto> {

    @Override
    @Mapping(source = "contacts", target = "contact")
    LegalGuardianApiDto convert(LegalGuardian source);

    @DelegatingConverter
    @InheritInverseConfiguration
    LegalGuardian fromApiDto(LegalGuardianApiDto apiDto);

}
