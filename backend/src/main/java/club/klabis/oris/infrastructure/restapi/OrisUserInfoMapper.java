package club.klabis.oris.infrastructure.restapi;

import club.klabis.oris.application.apiclient.dto.OrisUserInfo;
import club.klabis.oris.infrastructure.restapi.dto.ORISUserInfoApiDto;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
public interface OrisUserInfoMapper extends Converter<OrisUserInfo, ORISUserInfoApiDto> {

    @Mapping(target = "registrationNumber", ignore = true)
    @Override
    ORISUserInfoApiDto convert(OrisUserInfo source);
}
