package club.klabis.members.adapters.restapi.mappers;

import club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto;
import club.klabis.members.domain.forms.MemberEditForm;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
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
