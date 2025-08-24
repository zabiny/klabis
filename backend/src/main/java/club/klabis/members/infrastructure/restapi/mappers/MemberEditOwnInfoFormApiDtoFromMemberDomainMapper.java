package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.infrastructure.restapi.dto.EditMyDetailsFormApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberEditOwnInfoFormApiDtoFromMemberDomainMapper extends Converter<EditOwnMemberInfoForm, EditMyDetailsFormApiDto> {
    @Override
    EditMyDetailsFormApiDto convert(EditOwnMemberInfoForm source);

    @InheritInverseConfiguration
    @DelegatingConverter
    EditOwnMemberInfoForm convertReverse(EditMyDetailsFormApiDto source);

    @Mapping(target = "guardians", source = "legalGuardians")
    @Mapping(target = "contact", source = "contact")
    @DelegatingConverter
    EditOwnMemberInfoForm fromDomain(Member member);
}
