package club.klabis.adapters.api.mappers.appusers;

import club.klabis.api.dto.GlobalGrantDetailApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.appusers.ApplicationGrant;
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
