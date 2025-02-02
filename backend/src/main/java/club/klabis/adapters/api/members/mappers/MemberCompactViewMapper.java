package club.klabis.adapters.api.members.mappers;

import club.klabis.adapters.api.members.MembersController;
import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public abstract class MemberCompactViewMapper extends BaseMemberMapper<MemberViewCompactApiDto> implements Converter<Member, MemberViewCompactApiDto> {

    public MemberCompactViewMapper() {
        super(MembersController.class, MemberViewCompactApiDto.class);
    }

    @Mapping(source = "registration", target = "registrationNumber")
    @Override
    abstract public MemberViewCompactApiDto toModel(Member entity);
    
}
