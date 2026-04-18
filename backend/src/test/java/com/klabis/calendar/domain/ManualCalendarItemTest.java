package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ManualCalendarItem")
class ManualCalendarItemTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create manual item with all fields")
        void shouldCreateManualItemWithAllFields() {
            var command = new CalendarItem.CreateCalendarItem(
                    "Club Training", "Weekly at Park",
                    LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15));

            ManualCalendarItem item = ManualCalendarItem.create(command);

            assertThat(item).isNotNull();
            assertThat(item).isInstanceOf(ManualCalendarItem.class);
            assertThat(item.getId()).isNotNull();
            assertThat(item.getName()).isEqualTo("Club Training");
            assertThat(item.getDescription()).isEqualTo("Weekly at Park");
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            var command = new CalendarItem.CreateCalendarItem(
                    null, "desc", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15));

            assertThatThrownBy(() -> ManualCalendarItem.create(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            var command = new CalendarItem.CreateCalendarItem(
                    "   ", "desc", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15));

            assertThatThrownBy(() -> ManualCalendarItem.create(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when startDate is null")
        void shouldFailWhenStartDateIsNull() {
            var command = new CalendarItem.CreateCalendarItem(
                    "Name", null, null, LocalDate.of(2026, 6, 15));

            assertThatThrownBy(() -> ManualCalendarItem.create(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should fail when endDate is null")
        void shouldFailWhenEndDateIsNull() {
            var command = new CalendarItem.CreateCalendarItem(
                    "Name", null, LocalDate.of(2026, 6, 15), null);

            assertThatThrownBy(() -> ManualCalendarItem.create(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should fail when endDate is before startDate")
        void shouldFailWhenEndDateBeforeStartDate() {
            var command = new CalendarItem.CreateCalendarItem(
                    "Name", null,
                    LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 10));

            assertThatThrownBy(() -> ManualCalendarItem.create(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be on or after start date");
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should mutate fields on update")
        void shouldMutateFieldsOnUpdate() {
            ManualCalendarItem item = ManualCalendarItem.create(
                    new CalendarItem.CreateCalendarItem(
                            "Old", "old desc",
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)));

            item.update(new CalendarItem.UpdateCalendarItem(
                    "New", "new desc",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5)));

            assertThat(item.getName()).isEqualTo("New");
            assertThat(item.getDescription()).isEqualTo("new desc");
            assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        }

        @Test
        @DisplayName("should fail update when name is null")
        void shouldFailUpdateWhenNameIsNull() {
            ManualCalendarItem item = ManualCalendarItem.create(
                    new CalendarItem.CreateCalendarItem(
                            "Name", null,
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)));

            assertThatThrownBy(() -> item.update(new CalendarItem.UpdateCalendarItem(
                    null, null,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }
    }

    @Nested
    @DisplayName("assertCanBeDeleted()")
    class AssertCanBeDeletedTests {

        @Test
        @DisplayName("should return normally — manual items can always be deleted")
        void shouldReturnNormally() {
            ManualCalendarItem item = ManualCalendarItem.create(
                    new CalendarItem.CreateCalendarItem(
                            "Name", null,
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)));

            assertThatCode(item::assertCanBeDeleted).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("should reconstruct without validation")
        void shouldReconstructWithoutValidation() {
            CalendarItemId id = CalendarItemId.generate();

            ManualCalendarItem item = ManualCalendarItem.reconstruct(
                    id, "Name", "desc",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), null);

            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getName()).isEqualTo("Name");
        }
    }
}
