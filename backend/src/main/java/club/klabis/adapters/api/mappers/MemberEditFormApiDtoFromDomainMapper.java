package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.MemberEditFormApiDto;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.forms.MemberEditForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
interface MemberEditFormApiDtoFromDomainMapper extends Converter<Member, MemberEditFormApiDto> {

    @Mapping(target = "guardians", source = "legalGuardians")
    @Override
    MemberEditFormApiDto convert(Member source);
}
