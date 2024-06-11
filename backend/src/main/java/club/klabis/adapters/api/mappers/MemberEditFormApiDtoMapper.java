package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.MemberEditFormApiDto;
import club.klabis.api.dto.MembersMemberIdEditMemberInfoFormPostRequestApiDto;
import club.klabis.domain.members.forms.MemberEditForm;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = ApiDtoToDomainMapperConfiguration.class)
interface MemberEditFormApiDtoMapper extends Converter<MemberEditFormApiDto, MemberEditForm> {

}
