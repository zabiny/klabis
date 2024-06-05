package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.MemberRegistrationFormApiDto;
import club.klabis.domain.members.forms.RegistrationForm;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = ApiDtoMapperConfiguration.class)
interface MemberRegistrationFormApiDtoMapper extends Converter<MemberRegistrationFormApiDto, RegistrationForm> {

}
