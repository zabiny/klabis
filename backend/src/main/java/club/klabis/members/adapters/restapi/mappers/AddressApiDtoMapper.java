package club.klabis.members.adapters.restapi.mappers;

import club.klabis.api.dto.AddressApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.Address;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface AddressApiDtoMapper extends Converter<Address, AddressApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    Address fromApiDto(AddressApiDto apiDto);

}
