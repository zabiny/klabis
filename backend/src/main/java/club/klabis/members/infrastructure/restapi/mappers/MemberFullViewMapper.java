package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.LicencesApiDto;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring", config = DomainToDtoMapperConfiguration.class)
public abstract class MemberFullViewMapper implements Converter<Member, MembersApiResponse> {

    @Override
    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(target = "licences", source = ".")
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "member", source = ".")
    public abstract MembersApiResponse convert(Member member);

    @Mapping(source = "trainerLicence", target = "trainer")
    @Mapping(source = "refereeLicence", target = "referee")
    @Mapping(source = "obLicence", target = "ob")
    public abstract LicencesApiDto toLicencesApiDto(Member member);

}
