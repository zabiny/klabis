package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.events.EventCategory;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * MapStruct mapper for {@link Event} to {@link EventSummaryDto}. Separate from
 * {@link EventDtoConverter} because Spring registers one {@code Converter<S,T>} per bean; the
 * shared nested-mapping steps are duplicated here rather than pulled in via {@code uses=}
 * (see backend-patterns "never uses= on a Converter").
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface EventSummaryDtoConverter extends Converter<Event, EventSummaryDto> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "eventTypeId", source = "eventTypeId")
    @Mapping(target = "cancellationReason", source = "cancellationReason")
    @Mapping(target = "deadlines", source = "registrationDeadlines")
    EventSummaryDto convert(Event event);

    default UUID toUuid(MemberId memberId) {
        return memberId == null ? null : memberId.value();
    }

    default UUID unwrapEventTypeId(Optional<EventTypeId> eventTypeId) {
        return eventTypeId.map(EventTypeId::value).orElse(null);
    }

    default String unwrap(Optional<String> value) {
        return value.orElse(null);
    }

    default String toUrl(WebsiteUrl websiteUrl) {
        return websiteUrl == null ? null : websiteUrl.value();
    }

    EventStatus toApiStatus(com.klabis.events.domain.EventStatus status);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "fee", source = "feeOverride")
    EventCategoryDto toCategoryDto(EventCategory category);

    @Mapping(target = "amount", expression = "java(fee.amount())")
    @Mapping(target = "currency", expression = "java(fee.currency().getCurrencyCode())")
    EntryFeeDto toEntryFeeDto(Money fee);

    default List<LocalDate> toDeadlineList(RegistrationDeadlines deadlines) {
        if (deadlines == null || deadlines.isEmpty()) {
            return null;
        }
        return Stream.of(
                        deadlines.deadline1().orElse(null),
                        deadlines.deadline2().orElse(null),
                        deadlines.deadline3().orElse(null))
                .filter(d -> d != null)
                .toList();
    }
}
