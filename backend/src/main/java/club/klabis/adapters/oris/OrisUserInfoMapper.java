package club.klabis.adapters.oris;

import club.klabis.common.DomainToDtoMapperConfiguration;
import club.klabis.api.dto.ORISUserInfoApiDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public interface OrisUserInfoMapper extends Converter<OrisService.OrisUserInfo, ORISUserInfoApiDto> {

    @Mapping(target = "registrationNumber", ignore = true)
    @Override
    ORISUserInfoApiDto convert(OrisService.OrisUserInfo source);
}
