package club.klabis.adapters.api.mappers.members;

import club.klabis.api.dto.AddressApiDto;
import club.klabis.common.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.Address;
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
