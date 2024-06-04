package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.common.ConversionServiceAdapter;
import club.klabis.domain.members.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring", uses = ConversionServiceAdapter.class, unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MemberCompactViewMapper extends Converter<Member, MemberViewCompactApiDto> {

    @Mapping(source = "registration", target = "registrationNumber")
    @Override
    MemberViewCompactApiDto convert(Member source);
}
