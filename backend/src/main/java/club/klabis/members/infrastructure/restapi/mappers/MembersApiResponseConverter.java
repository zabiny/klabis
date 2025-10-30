package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.dto.LicencesApiDto;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.users.domain.ApplicationUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

@Mapper(componentModel = "spring", config = DomainToDtoMapperConfiguration.class)
public abstract class MembersApiResponseConverter implements Converter<Member, MembersApiResponse> {

    @Override
    @Mapping(source = "registration", target = "registrationNumber")
    @Mapping(target = "licences", source = ".")
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "member", source = ".")
    @Mapping(target = "userId", source = "appUserId")
    public abstract MembersApiResponse convert(Member member);

    @Mapping(source = "trainerLicence", target = "trainer")
    @Mapping(source = "refereeLicence", target = "referee")
    @Mapping(source = "obLicence", target = "ob")
    public abstract LicencesApiDto toLicencesApiDto(Member member);

    public Integer convert(Optional<ApplicationUser.Id> value) {
        return value.map(ApplicationUser.Id::value).orElse(null);
    }
}
