package club.klabis.users.infrastructure.restapi.mappers;

import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.users.domain.ApplicationUser;
import club.klabis.users.infrastructure.restapi.dto.MemberGrantsFormApiDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberGrantsFormMapper extends Converter<ApplicationUser, MemberGrantsFormApiDto> {

    @Mapping(target = "grants", source = "globalGrants")
    @Override
    MemberGrantsFormApiDto convert(ApplicationUser source);
}
