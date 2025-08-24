package club.klabis.users.infrastructure.restapi.mappers;

import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.infrastructure.restapi.dto.GlobalGrantDetailApiDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface GrantDescriptionMapper extends Converter<ApplicationGrant, GlobalGrantDetailApiDto> {

    @Mapping(target = "grant", source = ".")
    @Mapping(target = "description", source = "description")
    @Override
    GlobalGrantDetailApiDto convert(ApplicationGrant source);

}
