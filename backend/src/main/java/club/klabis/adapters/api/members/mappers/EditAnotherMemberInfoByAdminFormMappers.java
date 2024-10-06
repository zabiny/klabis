package club.klabis.adapters.api.members.mappers;

import club.klabis.api.dto.EditAnotherMemberDetailsFormApiDto;
import club.klabis.common.mapstruct.DomainFormMapperConfiguration;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.forms.EditAnotherMemberInfoByAdminForm;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainFormMapperConfiguration.class, unmappedSourcePolicy = ReportingPolicy.IGNORE)
interface EditAnotherMemberInfoByAdminFormMappers extends Converter<EditAnotherMemberInfoByAdminForm, EditAnotherMemberDetailsFormApiDto> {

    @Override
    EditAnotherMemberDetailsFormApiDto convert(EditAnotherMemberInfoByAdminForm source);

    @InheritInverseConfiguration
    @DelegatingConverter
    EditAnotherMemberInfoByAdminForm convertReverse(EditAnotherMemberDetailsFormApiDto source);

    @DelegatingConverter
    EditAnotherMemberInfoByAdminForm fromDomain(Member member);
}
