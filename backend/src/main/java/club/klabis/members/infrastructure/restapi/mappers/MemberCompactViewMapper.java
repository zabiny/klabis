package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MemberViewCompactApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring", config = DomainToDtoMapperConfiguration.class)
public abstract class MemberCompactViewMapper implements Converter<Member, MemberViewCompactApiDto> {

    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(source = "id.value", target = "id")
    @Override
    public abstract MemberViewCompactApiDto convert(Member member);

}
