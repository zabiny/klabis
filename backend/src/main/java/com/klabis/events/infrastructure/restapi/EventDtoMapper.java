package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;
import com.klabis.events.EventCategory;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

class EventDtoMapper {

    static EventDto toDto(Event event) {
        return new EventDto(
                toEntryFeeDto(event),
                event.getCancellationReason().orElse(null),
                toCategoryDtos(event),
                toCoordinatorIds(event),
                toDeadlineList(event.getRegistrationDeadlines()),
                event.getEventDate(),
                event.getEventTypeId().map(id -> id.value()).orElse(null),
                event.getId().value(),
                event.getLocation(),
                event.getName(),
                event.getOrganizer(),
                toRankingDto(event),
                toApiStatus(event.getStatus()),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null
        );
    }

    static EventSummaryDto toSummaryDto(Event event) {
        return new EventSummaryDto(
                event.getCancellationReason().orElse(null),
                toCategoryDtos(event),
                toCoordinatorIds(event),
                toDeadlineList(event.getRegistrationDeadlines()),
                event.getEventDate(),
                event.getEventTypeId().map(id -> id.value()).orElse(null),
                event.getId().value(),
                event.getLocation(),
                event.getName(),
                event.getOrganizer(),
                toApiStatus(event.getStatus()),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null
        );
    }

    private static EventStatus toApiStatus(com.klabis.events.domain.EventStatus status) {
        return status == null ? null : EventStatus.valueOf(status.name());
    }

    private static List<UUID> toCoordinatorIds(Event event) {
        return event.getCoordinators().stream().map(MemberId::value).toList();
    }

    private static List<EventCategoryDto> toCategoryDtos(Event event) {
        return event.getCategories().stream()
                .map(EventDtoMapper::toCategoryDto)
                .toList();
    }

    private static EventCategoryDto toCategoryDto(EventCategory category) {
        return new EventCategoryDto(category.fee().map(EventDtoMapper::toEntryFeeDto).orElse(null), category.id().value(), category.name());
    }

    private static EntryFeeDto toEntryFeeDto(Money fee) {
        return new EntryFeeDto(fee.amount(), fee.currency().getCurrencyCode());
    }

    private static RankingDto toRankingDto(Event event) {
        if (event.getRanking() == null) {
            return null;
        }
        return new RankingDto(event.getRanking().name(), event.getRanking().shortName());
    }

    private static EntryFeeDto toEntryFeeDto(Event event) {
        if (event.getBaseEntryFee() == null) {
            return null;
        }
        return new EntryFeeDto(
                event.getBaseEntryFee().amount(),
                event.getBaseEntryFee().currency().getCurrencyCode()
        );
    }

    private static List<LocalDate> toDeadlineList(RegistrationDeadlines deadlines) {
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

    private EventDtoMapper() {
    }
}
