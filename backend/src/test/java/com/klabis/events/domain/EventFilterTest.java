package com.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventFilter")
class EventFilterTest {

    @Nested
    @DisplayName("requestsOnlyStatus()")
    class RequestsOnlyStatusTests {

        @Test
        @DisplayName("returns true when filter has exactly that one status")
        void returnsTrueForSingleMatchingStatus() {
            assertThat(EventFilter.byStatus(EventStatus.DRAFT).requestsOnlyStatus(EventStatus.DRAFT)).isTrue();
        }

        @Test
        @DisplayName("returns false when filter has multiple statuses")
        void returnsFalseForMultipleStatuses() {
            assertThat(EventFilter.byStatus(EventStatus.DRAFT, EventStatus.ACTIVE).requestsOnlyStatus(EventStatus.DRAFT)).isFalse();
        }

        @Test
        @DisplayName("returns false for none-filter (empty set)")
        void returnsFalseForNoneFilter() {
            assertThat(EventFilter.none().requestsOnlyStatus(EventStatus.DRAFT)).isFalse();
        }

        @Test
        @DisplayName("returns false when filter has single different status")
        void returnsFalseForDifferentStatus() {
            assertThat(EventFilter.byStatus(EventStatus.ACTIVE).requestsOnlyStatus(EventStatus.DRAFT)).isFalse();
        }
    }

    @Nested
    @DisplayName("excludesStatus()")
    class ExcludesStatusTests {

        @Test
        @DisplayName("returns true when filter has explicit statuses not including DRAFT")
        void returnsTrueWhenStatusNotInExplicitSet() {
            assertThat(EventFilter.byStatus(EventStatus.ACTIVE, EventStatus.FINISHED).excludesStatus(EventStatus.DRAFT)).isTrue();
        }

        @Test
        @DisplayName("returns true for byNotHavingStatus filter")
        void returnsTrueForByNotHavingStatusFilter() {
            assertThat(EventFilter.byNotHavingStatus(EventStatus.DRAFT).excludesStatus(EventStatus.DRAFT)).isTrue();
        }

        @Test
        @DisplayName("returns false for none-filter (no restriction applied yet)")
        void returnsFalseForNoneFilter() {
            assertThat(EventFilter.none().excludesStatus(EventStatus.DRAFT)).isFalse();
        }

        @Test
        @DisplayName("returns false when filter explicitly includes that status")
        void returnsFalseWhenStatusInSet() {
            assertThat(EventFilter.byStatus(EventStatus.DRAFT).excludesStatus(EventStatus.DRAFT)).isFalse();
        }
    }

    @Nested
    @DisplayName("activeEventsWithDateBefore()")
    class ActiveEventsWithDateBeforeTests {

        @Test
        @DisplayName("statuses contains only ACTIVE")
        void statusesContainsOnlyActive() {
            EventFilter filter = EventFilter.activeEventsWithDateBefore(java.time.LocalDate.of(2026, 5, 10));
            assertThat(filter.statuses()).containsExactly(EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("dateTo is date minus one day — preserving exclusive-upper-bound semantics")
        void dateToIsOneDayBeforeDate() {
            EventFilter filter = EventFilter.activeEventsWithDateBefore(java.time.LocalDate.of(2026, 5, 10));
            assertThat(filter.dateTo()).isEqualTo(java.time.LocalDate.of(2026, 5, 9));
        }

        @Test
        @DisplayName("dateFrom and organizer are null — no restriction on those dimensions")
        void dateFromAndOrganizerAreNull() {
            EventFilter filter = EventFilter.activeEventsWithDateBefore(java.time.LocalDate.of(2026, 5, 10));
            assertThat(filter.dateFrom()).isNull();
            assertThat(filter.organizer()).isNull();
        }
    }

    @Nested
    @DisplayName("withFulltext()")
    class WithFulltextTests {

        @Test
        @DisplayName("stores the trimmed query")
        void storesTrimmedQuery() {
            EventFilter filter = EventFilter.none().withFulltext("  jihlava  ");
            assertThat(filter.fulltextQuery()).isEqualTo("jihlava");
        }

        @Test
        @DisplayName("stores null when query is blank after trim")
        void storesNullForBlankQuery() {
            EventFilter filter = EventFilter.none().withFulltext("   ");
            assertThat(filter.fulltextQuery()).isNull();
        }

        @Test
        @DisplayName("stores null when called with null")
        void storesNullForNullInput() {
            EventFilter filter = EventFilter.none().withFulltext(null);
            assertThat(filter.fulltextQuery()).isNull();
        }

        @Test
        @DisplayName("preserves all other filter dimensions")
        void preservesOtherDimensions() {
            EventFilter base = EventFilter.byOrganizer("OOB");
            EventFilter result = base.withFulltext("jihlava");
            assertThat(result.organizer()).isEqualTo("OOB");
            assertThat(result.statuses()).isEmpty();
            assertThat(result.dateFrom()).isNull();
            assertThat(result.dateTo()).isNull();
        }

        @Test
        @DisplayName("none-filter has null fulltextQuery by default")
        void noneFilterHasNullFulltextQuery() {
            assertThat(EventFilter.none().fulltextQuery()).isNull();
        }
    }

    @Nested
    @DisplayName("withExcludedStatus()")
    class WithExcludedStatusTests {

        @Test
        @DisplayName("none-filter becomes byNotHavingStatus when DRAFT is excluded")
        void noneFilterBecomesComplement() {
            EventFilter result = EventFilter.none().withExcludedStatus(EventStatus.DRAFT);
            assertThat(result).isEqualTo(EventFilter.byNotHavingStatus(EventStatus.DRAFT));
        }

        @Test
        @DisplayName("removes DRAFT from a multi-status filter leaving remaining statuses")
        void removesStatusFromMultiStatusFilter() {
            EventFilter result = EventFilter.byStatus(EventStatus.DRAFT, EventStatus.ACTIVE, EventStatus.FINISHED)
                    .withExcludedStatus(EventStatus.DRAFT);
            assertThat(result.statuses()).containsExactlyInAnyOrder(EventStatus.ACTIVE, EventStatus.FINISHED);
        }

        @Test
        @DisplayName("preserves other filter dimensions (organizer, dates) when removing status")
        void preservesOtherDimensions() {
            EventFilter base = new EventFilter(
                    java.util.Set.of(EventStatus.DRAFT, EventStatus.ACTIVE),
                    "OOB",
                    java.time.LocalDate.of(2026, 1, 1),
                    java.time.LocalDate.of(2026, 12, 31),
                    null,
                    null
            );
            EventFilter result = base.withExcludedStatus(EventStatus.DRAFT);
            assertThat(result.organizer()).isEqualTo("OOB");
            assertThat(result.dateFrom()).isEqualTo(java.time.LocalDate.of(2026, 1, 1));
            assertThat(result.dateTo()).isEqualTo(java.time.LocalDate.of(2026, 12, 31));
            assertThat(result.statuses()).containsExactly(EventStatus.ACTIVE);
        }
    }
}
