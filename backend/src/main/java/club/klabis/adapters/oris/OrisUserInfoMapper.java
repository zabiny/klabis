package club.klabis.adapters.oris;

import club.klabis.adapters.api.mappers.DomainToApiDtoMapperConfiguration;
import club.klabis.api.dto.ORISUserInfoApiDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
public interface OrisUserInfoMapper extends Converter<OrisService.OrisUserInfo, ORISUserInfoApiDto> {

    @Mapping(target = "registrationNumber", ignore = true)
    @Override
    ORISUserInfoApiDto convert(OrisService.OrisUserInfo source);
}
