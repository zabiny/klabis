package club.klabis.appusers.restapi.mappers;

import club.klabis.api.dto.MemberGrantsFormApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.appusers.domain.ApplicationUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberGrantsFormMapper extends Converter<ApplicationUser, MemberGrantsFormApiDto> {

    @Mapping(target = "grants", source = "globalGrants")
    @Override
    MemberGrantsFormApiDto convert(ApplicationUser source);
}