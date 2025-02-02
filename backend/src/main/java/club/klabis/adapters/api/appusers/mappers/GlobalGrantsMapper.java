package club.klabis.adapters.api.appusers.mappers;

import club.klabis.api.dto.GlobalGrantsApiDto;
import club.klabis.common.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.domain.appusers.ApplicationGrant;
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
