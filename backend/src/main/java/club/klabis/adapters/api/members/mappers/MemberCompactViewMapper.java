package club.klabis.adapters.api.members.mappers;

import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public interface MemberCompactViewMapper extends Converter<Member, MemberViewCompactApiDto> {

    @Mapping(source = "registration", target = "registrationNumber")
    @Override
    MemberViewCompactApiDto convert(Member source);
}
