package club.klabis.oris.adapters.restapi;

import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.api.dto.ORISUserInfoApiDto;
import club.klabis.oris.application.apiclient.dto.OrisUserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public interface OrisUserInfoMapper extends Converter<OrisUserInfo, ORISUserInfoApiDto> {

    @Mapping(target = "registrationNumber", ignore = true)
    @Override
    ORISUserInfoApiDto convert(OrisUserInfo source);
}
