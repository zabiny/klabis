package club.klabis.adapters.api.mappers.appusers;

import club.klabis.api.dto.MemberGrantsFormApiDto;
import club.klabis.common.DomainToDtoMapperConfiguration;
import club.klabis.domain.appusers.ApplicationUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberGrantsFormMapper extends Converter<ApplicationUser, MemberGrantsFormApiDto> {

    @Mapping(target = "grants", source = "applicationGrants")
    @Override
    MemberGrantsFormApiDto convert(ApplicationUser source);
}
