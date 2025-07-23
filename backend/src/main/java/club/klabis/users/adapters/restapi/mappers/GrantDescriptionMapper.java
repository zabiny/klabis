package club.klabis.users.adapters.restapi.mappers;

import club.klabis.api.dto.GlobalGrantDetailApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.users.domain.ApplicationGrant;
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
