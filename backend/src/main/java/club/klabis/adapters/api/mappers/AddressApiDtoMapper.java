package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.AddressApiDto;
import club.klabis.domain.members.Address;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.ERROR, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AddressApiDtoMapper extends Converter<Address, AddressApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    Address fromApiDto(AddressApiDto apiDto);

}
