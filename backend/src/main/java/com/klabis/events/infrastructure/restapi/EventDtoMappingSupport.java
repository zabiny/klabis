package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventCategory;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.Money;
import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

final class EventDtoMappingSupport {

    private EventDtoMappingSupport() {
    }

    static EventStatus toApiStatus(com.klabis.events.domain.EventStatus status) {
        return status == null ? null : EventStatus.valueOf(status.name());
    }

    static List<UUID> toCoordinatorIds(Event event) {
        return event.getCoordinators().stream().map(MemberId::value).toList();
    }

    static UUID toEventTypeId(Event event) {
        return event.getEventTypeId().map(com.klabis.events.EventTypeId::value).orElse(null);
    }

    static List<EventCategoryDto> toCategoryDtos(Event event) {
        return event.getCategories().stream()
                .map(EventDtoMappingSupport::toCategoryDto)
                .toList();
    }

    static EventCategoryDto toCategoryDto(EventCategory category) {
        return EventCategoryDtoBuilder.builder()
                .id(category.id().value())
                .name(category.name())
                .fee(category.fee().map(EventDtoMappingSupport::toEntryFeeDto).orElse(null))
                .build();
    }

    static EntryFeeDto toEntryFeeDto(Money fee) {
        return EntryFeeDtoBuilder.builder()
                .amount(fee.amount())
                .currency(fee.currency().getCurrencyCode())
                .build();
    }

    static EntryFeeDto toEntryFeeDto(Event event) {
        if (event.getBaseEntryFee() == null) {
            return null;
        }
        return toEntryFeeDto(event.getBaseEntryFee());
    }

    static RankingDto toRankingDto(Event event) {
        if (event.getRanking() == null) {
            return null;
        }
        return RankingDtoBuilder.builder()
                .shortName(event.getRanking().shortName())
                .name(event.getRanking().name())
                .build();
    }

    static List<LocalDate> toDeadlineList(RegistrationDeadlines deadlines) {
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
