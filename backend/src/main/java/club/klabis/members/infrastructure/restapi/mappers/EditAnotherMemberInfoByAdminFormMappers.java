package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.infrastructure.restapi.dto.EditAnotherMemberDetailsFormApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class, unmappedSourcePolicy = ReportingPolicy.IGNORE)
interface EditAnotherMemberInfoByAdminFormMappers extends Converter<EditAnotherMemberInfoByAdminForm, EditAnotherMemberDetailsFormApiDto> {

    @Override
    EditAnotherMemberDetailsFormApiDto convert(EditAnotherMemberInfoByAdminForm source);

    @InheritInverseConfiguration
    @DelegatingConverter
    EditAnotherMemberInfoByAdminForm convertReverse(EditAnotherMemberDetailsFormApiDto source);

    @DelegatingConverter
    EditAnotherMemberInfoByAdminForm fromDomain(Member member);
}
