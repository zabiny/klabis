package com.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventCategoryTest {

    @Nested
    @DisplayName("EventCategoryId")
    class EventCategoryIdTests {

        @Test
        @DisplayName("should generate unique ids")
        void shouldGenerateUniqueIds() {
            EventCategoryId first = EventCategoryId.generate();
            EventCategoryId second = EventCategoryId.generate();

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("should be equal when value is equal")
        void shouldBeEqualByValue() {
            java.util.UUID value = java.util.UUID.randomUUID();

            assertThat(new EventCategoryId(value)).isEqualTo(new EventCategoryId(value));
        }

        @Test
        @DisplayName("should reject null value")
        void shouldRejectNullValue() {
            assertThatThrownBy(() -> new EventCategoryId(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("EventCategory")
    class EventCategoryCreationTests {

        @Test
        @DisplayName("should create with name only")
        void shouldCreateWithNameOnly() {
            EventCategory category = EventCategory.create("M21");

            assertThat(category.id()).isNotNull();
            assertThat(category.name()).isEqualTo("M21");
            assertThat(category.orisId()).isNull();
            assertThat(category.fee()).isEmpty();
        }

        @Test
        @DisplayName("should accept optional orisId and fee")
        void shouldAcceptOptionalOrisIdAndFee() {
            Money fee = Money.ofCzk(BigDecimal.valueOf(200));

            EventCategory category = new EventCategory(EventCategoryId.generate(), "42", "M21", fee);

            assertThat(category.orisId()).isEqualTo("42");
            assertThat(category.fee()).contains(fee);
        }

        @Test
        @DisplayName("should require a name")
        void shouldRequireName() {
            assertThatThrownBy(() -> new EventCategory(EventCategoryId.generate(), null, "  ", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should require an id")
        void shouldRequireId() {
            assertThatThrownBy(() -> new EventCategory(null, null, "M21", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
