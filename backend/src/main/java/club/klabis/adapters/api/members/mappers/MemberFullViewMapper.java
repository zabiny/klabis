package club.klabis.adapters.api.members.mappers;

import club.klabis.adapters.api.members.MembersController;
import club.klabis.api.dto.LicencesApiDto;
import club.klabis.api.dto.MemberApiDto;
import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.common.ConversionServiceAdapter;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.common.mapstruct.OptionalMapstructSupport;
import club.klabis.domain.members.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
//@Mapper(componentModel = "spring", uses = {ConversionServiceAdapter.class, OptionalMapstructSupport.class})
public abstract class MemberFullViewMapper extends BaseMemberMapper<MemberApiDto> {


    public MemberFullViewMapper() {
        super(MembersController.class, MemberApiDto.class);
    }

    @Override
    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(target = "licences", source = ".")
    public abstract MemberApiDto toModel(Member source);

    @Mapping(source = "trainerLicence", target = "trainer")
    @Mapping(source = "refereeLicence", target = "referee")
    @Mapping(source = "obLicence", target = "ob")
    public abstract LicencesApiDto toLicencesApiDto(Member member);

}
