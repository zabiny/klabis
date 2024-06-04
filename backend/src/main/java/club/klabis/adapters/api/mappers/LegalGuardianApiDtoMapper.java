package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.LegalGuardianApiDto;
import club.klabis.domain.members.LegalGuardian;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.ERROR, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface LegalGuardianApiDtoMapper extends Converter<LegalGuardian, LegalGuardianApiDto> {

    @DelegatingConverter
    @InheritInverseConfiguration
    LegalGuardian fromApiDto(LegalGuardianApiDto apiDto);

}
