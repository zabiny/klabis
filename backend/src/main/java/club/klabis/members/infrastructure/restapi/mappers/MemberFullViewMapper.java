package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.EditMemberUseCaseControllers;
import club.klabis.members.infrastructure.restapi.dto.LicencesApiDto;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = DomainToDtoMapperConfiguration.class)
//@Mapper(componentModel = "spring", uses = {ConversionServiceAdapter.class, OptionalMapstructSupport.class})
public abstract class MemberFullViewMapper extends BaseMemberMapper<MembersApiResponse> {


    public MemberFullViewMapper() {
        super(EditMemberUseCaseControllers.class, MembersApiResponse.class);
    }

    @Override
    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(target = "licences", source = ".")
    @Mapping(target = "id", source = "id.value")
    public abstract MembersApiResponse toModel(Member source);

    @Mapping(source = "trainerLicence", target = "trainer")
    @Mapping(source = "refereeLicence", target = "referee")
    @Mapping(source = "obLicence", target = "ob")
    public abstract LicencesApiDto toLicencesApiDto(Member member);

}
