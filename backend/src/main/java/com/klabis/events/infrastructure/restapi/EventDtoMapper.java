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
        return EventDtoBuilder.builder()
                .id(event.getId().value())
                .name(event.getName())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .organizer(event.getOrganizer())
                .websiteUrl(event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null)
                .coordinators(toCoordinatorIds(event))
                .eventTypeId(event.getEventTypeId().map(id -> id.value()).orElse(null))
                .status(toApiStatus(event.getStatus()))
                .categories(toCategoryDtos(event))
                .cancellationReason(event.getCancellationReason().orElse(null))
                .deadlines(toDeadlineList(event.getRegistrationDeadlines()))
                .ranking(toRankingDto(event))
                .baseEntryFee(toEntryFeeDto(event))
                .build();
    }

    static EventSummaryDto toSummaryDto(Event event) {
        return EventSummaryDtoBuilder.builder()
                .id(event.getId().value())
                .name(event.getName())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .organizer(event.getOrganizer())
                .websiteUrl(event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null)
                .coordinators(toCoordinatorIds(event))
                .eventTypeId(event.getEventTypeId().map(id -> id.value()).orElse(null))
                .status(toApiStatus(event.getStatus()))
                .categories(toCategoryDtos(event))
                .cancellationReason(event.getCancellationReason().orElse(null))
                .deadlines(toDeadlineList(event.getRegistrationDeadlines()))
                .build();
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
        return EventCategoryDtoBuilder.builder()
                .id(category.id().value())
                .name(category.name())
                .fee(category.fee().map(EventDtoMapper::toEntryFeeDto).orElse(null))
                .build();
    }

    private static EntryFeeDto toEntryFeeDto(Money fee) {
        return EntryFeeDtoBuilder.builder()
                .amount(fee.amount())
                .currency(fee.currency().getCurrencyCode())
                .build();
    }

    private static RankingDto toRankingDto(Event event) {
        if (event.getRanking() == null) {
            return null;
        }
        return RankingDtoBuilder.builder()
                .shortName(event.getRanking().shortName())
                .name(event.getRanking().name())
                .build();
    }

    private static EntryFeeDto toEntryFeeDto(Event event) {
        if (event.getBaseEntryFee() == null) {
            return null;
        }
        return EntryFeeDtoBuilder.builder()
                .amount(event.getBaseEntryFee().amount())
                .currency(event.getBaseEntryFee().currency().getCurrencyCode())
                .build();
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
