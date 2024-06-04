package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.MemberRegistrationFormApiDto;
import club.klabis.common.ConversionServiceAdapter;
import club.klabis.domain.members.forms.RegistrationForm;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring", uses = ConversionServiceAdapter.class, unmappedSourcePolicy = ReportingPolicy.ERROR, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface MemberRegistrationFormApiDtoMapper extends Converter<MemberRegistrationFormApiDto, RegistrationForm> {

}
