package club.klabis.members.restapi.mappers;

import club.klabis.api.dto.LicencesApiDto;
import club.klabis.api.dto.MemberApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberFullViewMapper extends Converter<Member, MemberApiDto> {


    @Override
    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(target = "licences", source = ".")
    @Mapping(target = "medicCourse", ignore = true)
    MemberApiDto convert(Member source);

    @Mapping(source = "trainerLicence", target = "trainer")
    @Mapping(source = "refereeLicence", target = "referee")
    @Mapping(source = "obLicence", target = "ob")
    LicencesApiDto toLicencesApiDto(Member member);

}