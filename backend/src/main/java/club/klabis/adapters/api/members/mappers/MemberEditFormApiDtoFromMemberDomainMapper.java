package club.klabis.adapters.api.members.mappers;

import club.klabis.api.dto.MemberEditFormApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberEditFormApiDtoFromMemberDomainMapper extends Converter<Member, MemberEditFormApiDto> {

    @Mapping(target = "guardians", source = "legalGuardians")
    @Mapping(target = "medicCourse", ignore = true)
    @Override
    MemberEditFormApiDto convert(Member source);
}
