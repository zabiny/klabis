package club.klabis.adapters.api.members.mappers;

import club.klabis.api.dto.EditMyDetailsFormApiDto;
import club.klabis.common.mapstruct.DomainFormMapperConfiguration;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.forms.EditOwnMemberInfoForm;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainFormMapperConfiguration.class)
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
