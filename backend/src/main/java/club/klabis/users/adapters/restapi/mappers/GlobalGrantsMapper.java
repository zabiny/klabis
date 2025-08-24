package club.klabis.users.adapters.restapi.mappers;

import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.users.adapters.restapi.dto.GlobalGrantsApiDto;
import club.klabis.users.domain.ApplicationGrant;
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
