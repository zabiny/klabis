package club.klabis.users.infrastructure.restapi.mappers;

import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.infrastructure.restapi.dto.GlobalGrantsApiDto;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface GlobalGrantsMapper extends Converter<ApplicationGrant, GlobalGrantsApiDto> {

    @Override
    default GlobalGrantsApiDto convert(ApplicationGrant source) {
        return GlobalGrantsApiDto.fromValue(source.getGrantName());
    }

    @DelegatingConverter
    default ApplicationGrant fromApiDto(GlobalGrantsApiDto apiDto) {
        return ApplicationGrant.fromGrantName(apiDto.getValue());
    }
}
