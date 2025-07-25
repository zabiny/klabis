package club.klabis.members.adapters.restapi.mappers;

import club.klabis.api.dto.MemberRegistrationFormApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.forms.RegistrationForm;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface MemberRegistrationFormApiDtoMapper extends Converter<RegistrationForm, MemberRegistrationFormApiDto> {
    @DelegatingConverter
    @InheritInverseConfiguration
    RegistrationForm reverseConvert(MemberRegistrationFormApiDto apiDto);

}
