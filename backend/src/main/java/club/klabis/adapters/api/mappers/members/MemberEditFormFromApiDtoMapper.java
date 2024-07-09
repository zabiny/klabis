package club.klabis.adapters.api.mappers.members;

import club.klabis.api.dto.MemberEditFormApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.members.forms.MemberEditForm;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberEditFormFromApiDtoMapper extends Converter<MemberEditForm, MemberEditFormApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    MemberEditForm reverseConvert(MemberEditFormApiDto apiDto);
}
