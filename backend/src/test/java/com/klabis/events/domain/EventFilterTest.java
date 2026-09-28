package com.klabis.events.domain;

import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Period;
import java.util.List;
import java.util.UUID;

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
    @DisplayName("withDeadlineWithin()")
    class WithDeadlineWithinTests {

        @Test
        @DisplayName("stores the period when set")
        void storesPeriod() {
            Period sevenDays = Period.ofDays(7);
            EventFilter filter = EventFilter.none().withDeadlineWithin(sevenDays);
            assertThat(filter.deadlineWithin()).isEqualTo(sevenDays);
        }

        @Test
        @DisplayName("stores null when cleared")
        void storesNullWhenCleared() {
            EventFilter filter = EventFilter.none().withDeadlineWithin(Period.ofDays(7)).withDeadlineWithin(null);
            assertThat(filter.deadlineWithin()).isNull();
        }

        @Test
        @DisplayName("preserves all other filter dimensions")
        void preservesOtherDimensions() {
            EventFilter base = EventFilter.byStatus(EventStatus.ACTIVE).withOrganizer("OOB");
            EventFilter result = base.withDeadlineWithin(Period.ofDays(7));
            assertThat(result.statuses()).containsExactly(EventStatus.ACTIVE);
            assertThat(result.organizer()).isEqualTo("OOB");
            assertThat(result.notRegisteredBy()).isNull();
        }

        @Test
        @DisplayName("none-filter has null deadlineWithin by default")
        void noneFilterHasNullDeadlineWithin() {
            assertThat(EventFilter.none().deadlineWithin()).isNull();
        }
    }

    @Nested
    @DisplayName("withNotRegisteredBy()")
    class WithNotRegisteredByTests {

        @Test
        @DisplayName("stores the member ID when set")
        void storesMemberId() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter filter = EventFilter.none().withNotRegisteredBy(member);
            assertThat(filter.notRegisteredBy()).isEqualTo(member);
        }

        @Test
        @DisplayName("stores null when cleared")
        void storesNullWhenCleared() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter filter = EventFilter.none().withNotRegisteredBy(member).withNotRegisteredBy(null);
            assertThat(filter.notRegisteredBy()).isNull();
        }

        @Test
        @DisplayName("preserves all other filter dimensions")
        void preservesOtherDimensions() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter base = EventFilter.byStatus(EventStatus.ACTIVE).withDeadlineWithin(Period.ofDays(7));
            EventFilter result = base.withNotRegisteredBy(member);
            assertThat(result.statuses()).containsExactly(EventStatus.ACTIVE);
            assertThat(result.deadlineWithin()).isEqualTo(Period.ofDays(7));
            assertThat(result.registeredBy()).isNull();
        }

        @Test
        @DisplayName("none-filter has null notRegisteredBy by default")
        void noneFilterHasNullNotRegisteredBy() {
            assertThat(EventFilter.none().notRegisteredBy()).isNull();
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
                    null,
                    null,
                    null,
                    null,
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

    @Nested
    @DisplayName("withCancelledVisibleTo()")
    class WithCancelledVisibleToTests {

        @Test
        @DisplayName("sets the member and leaves the status set untouched")
        void setsMemberAndLeavesStatusesUntouched() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter result = EventFilter.byStatus(EventStatus.CANCELLED).withCancelledVisibleTo(member);
            assertThat(result.cancelledVisibleTo()).isEqualTo(member);
            assertThat(result.statuses()).containsExactly(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("null clears the restriction")
        void nullClearsTheRestriction() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter filter = EventFilter.none().withCancelledVisibleTo(member).withCancelledVisibleTo(null);
            assertThat(filter.cancelledVisibleTo()).isNull();
        }

        @Test
        @DisplayName("preserves every other filter dimension")
        void preservesOtherDimensions() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter base = EventFilter.byStatus(EventStatus.ACTIVE, EventStatus.CANCELLED)
                    .withOrganizer("OOB")
                    .withDateRange(java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31))
                    .withFulltext("jihlava")
                    .withRegisteredBy(member)
                    .withDeadlineWithin(Period.ofDays(7))
                    .withEventTypeIds(List.of());

            EventFilter result = base.withCancelledVisibleTo(member);

            assertThat(result.organizer()).isEqualTo("OOB");
            assertThat(result.dateFrom()).isEqualTo(java.time.LocalDate.of(2026, 1, 1));
            assertThat(result.dateTo()).isEqualTo(java.time.LocalDate.of(2026, 12, 31));
            assertThat(result.fulltextQuery()).isEqualTo("jihlava");
            assertThat(result.registeredBy()).isEqualTo(member);
            assertThat(result.deadlineWithin()).isEqualTo(Period.ofDays(7));
            assertThat(result.statuses()).containsExactlyInAnyOrder(EventStatus.ACTIVE, EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("every other copy-factory preserves an already-set member")
        void otherCopyFactoriesPreserveTheMember() {
            MemberId member = new MemberId(UUID.randomUUID());
            EventFilter base = EventFilter.none().withCancelledVisibleTo(member);

            assertThat(base.withExcludedStatus(EventStatus.DRAFT).cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withFulltext("jihlava").cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withOrganizer("OOB").cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withRegisteredBy(member).cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withCoordinator(member).cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withDeadlineWithin(Period.ofDays(7)).cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withNotRegisteredBy(member).cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withEventTypeIds(List.of()).cancelledVisibleTo()).isEqualTo(member);
            assertThat(base.withDateRange(java.time.LocalDate.of(2026, 1, 1), null).cancelledVisibleTo()).isEqualTo(member);
        }

        @Test
        @DisplayName("none-filter has null cancelledVisibleTo by default")
        void noneFilterHasNullCancelledVisibleTo() {
            assertThat(EventFilter.none().cancelledVisibleTo()).isNull();
        }
    }
}
