package club.klabis.oris.restapi;

import club.klabis.oris.client.OrisApiClient;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.api.dto.ORISUserInfoApiDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface OrisUserInfoMapper extends Converter<OrisApiClient.OrisUserInfo, ORISUserInfoApiDto> {

    @Mapping(target = "registrationNumber", ignore = true)
    @Override
    ORISUserInfoApiDto convert(OrisApiClient.OrisUserInfo source);
}