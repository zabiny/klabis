package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.MembersController;
import club.klabis.members.infrastructure.restapi.dto.MemberViewCompactApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public abstract class MemberCompactViewMapper extends BaseMemberMapper<MemberViewCompactApiDto> implements Converter<Member, MemberViewCompactApiDto> {

    public MemberCompactViewMapper() {
        super(MembersController.class, MemberViewCompactApiDto.class);
    }

    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(source = "id.value", target = "id")
    @Override
    abstract public MemberViewCompactApiDto toModel(Member entity);
    
}
