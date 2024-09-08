package club.klabis.adapters.api.appusers.mappers;

import club.klabis.api.dto.GlobalGrantsApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.appusers.ApplicationGrant;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface GlobalGrantsMapper extends Converter<ApplicationGrant, GlobalGrantsApiDto> {

    @ValueMapping(source = "MEMBERS_REGISTER", target = "REGISTER")
    @ValueMapping(source = "MEMBERS_EDIT", target = "EDIT")
    @ValueMapping(source = "MEMBERS_SUSPENDMEMBERSHIP", target = "SUSPENDMEMBERSHIP")
    @ValueMapping(source = "APPUSERS_PERMISSIONS", target = "PERMISSIONS")
    @Override
    GlobalGrantsApiDto convert(ApplicationGrant source);

    @DelegatingConverter
    @InheritInverseConfiguration
    ApplicationGrant fromApiDto(GlobalGrantsApiDto apiDto);
}
