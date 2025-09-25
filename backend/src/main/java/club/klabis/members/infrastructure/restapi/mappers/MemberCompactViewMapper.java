package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.MemberViewCompactApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = DomainToDtoMapperConfiguration.class)
public abstract class MemberCompactViewMapper extends BaseMemberMapper<MemberViewCompactApiDto> {

    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(source = "id.value", target = "id")
    @Override
    public abstract MemberViewCompactApiDto mapDataFromDomain(Member member);

}
