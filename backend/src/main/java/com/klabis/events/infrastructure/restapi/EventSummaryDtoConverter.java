package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.domain.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface EventSummaryDtoConverter extends Converter<Event, EventSummaryDto> {

    @Override
    @Mapping(target = "id", expression = "java(event.getId().value())")
    @Mapping(target = "websiteUrl", expression = "java(event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null)")
    @Mapping(target = "coordinators", expression = "java(EventDtoMappingSupport.toCoordinatorIds(event))")
    @Mapping(target = "eventTypeId", expression = "java(EventDtoMappingSupport.toEventTypeId(event))")
    @Mapping(target = "status", expression = "java(EventDtoMappingSupport.toApiStatus(event.getStatus()))")
    @Mapping(target = "categories", expression = "java(EventDtoMappingSupport.toCategoryDtos(event))")
    @Mapping(target = "cancellationReason", expression = "java(event.getCancellationReason().orElse(null))")
    @Mapping(target = "deadlines", expression = "java(EventDtoMappingSupport.toDeadlineList(event.getRegistrationDeadlines()))")
    EventSummaryDto convert(Event event);
}
