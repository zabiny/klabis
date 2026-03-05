package com.klabis.common.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TranslatedPageable}.
 */
class TranslatedPageableTest {

    @Test
    void shouldTranslateSingleSortProperty() {
        // Arrange
        Map<String, String> translationMap = Map.of(
                "eventDate", "event_date",
                "userName", "user_name"
        );

        Sort originalSort = Sort.by("eventDate").ascending();
        Pageable originalPageable = PageRequest.of(0, 10, originalSort);

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        assertThat(translated.getPageNumber()).isEqualTo(0);
        assertThat(translated.getPageSize()).isEqualTo(10);
        assertThat(translated.getSort()).isEqualTo(Sort.by("event_date").ascending());
    }

    @Test
    void shouldTranslateMultipleSortProperties() {
        // Arrange
        Map<String, String> translationMap = Map.of(
                "eventDate", "event_date",
                "name", "name",
                "status", "status"
        );

        Sort originalSort = Sort.by(
                Sort.Order.asc("eventDate"),
                Sort.Order.desc("name")
        );
        Pageable originalPageable = PageRequest.of(1, 20, originalSort);

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        assertThat(translated.getPageNumber()).isEqualTo(1);
        assertThat(translated.getPageSize()).isEqualTo(20);

        Sort expectedSort = Sort.by(
                Sort.Order.asc("event_date"),
                Sort.Order.desc("name")
        );
        assertThat(translated.getSort()).isEqualTo(expectedSort);
    }

    @Test
    void shouldHandleUnknownPropertyGracefully() {
        // Arrange
        Map<String, String> translationMap = Map.of(
                "eventDate", "event_date"
        );

        Sort originalSort = Sort.by("unknownProperty").ascending();
        Pageable originalPageable = PageRequest.of(0, 10, originalSort);

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        assertThat(translated.getSort()).isEqualTo(Sort.by("unknownProperty").ascending());
    }

    @Test
    void shouldPreserveNullHandling() {
        // Arrange
        Map<String, String> translationMap = Map.of(
                "eventDate", "event_date"
        );

        Sort originalSort = Sort.by(
                new Sort.Order(Sort.Direction.ASC, "eventDate", Sort.NullHandling.NULLS_FIRST)
        );
        Pageable originalPageable = PageRequest.of(0, 10, originalSort);

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        Sort.Order expectedOrder = new Sort.Order(Sort.Direction.ASC, "event_date", Sort.NullHandling.NULLS_FIRST);
        assertThat(translated.getSort()).isEqualTo(Sort.by(expectedOrder));
    }

    @Test
    void shouldHandleEmptySort() {
        // Arrange
        Map<String, String> translationMap = Map.of();
        Pageable originalPageable = PageRequest.of(0, 10, Sort.unsorted());

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        assertThat(translated.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldTranslateOnlySortPropertyWithoutChangingPagination() {
        // Arrange
        Map<String, String> translationMap = Map.of(
                "eventDate", "event_date"
        );

        Pageable originalPageable = PageRequest.of(5, 50, Sort.by("eventDate").descending());

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        assertThat(translated.getPageNumber()).isEqualTo(5);
        assertThat(translated.getPageSize()).isEqualTo(50);
        assertThat(translated.getOffset()).isEqualTo(250L); // 5 * 50
        assertThat(translated.getSort()).isEqualTo(Sort.by("event_date").descending());
    }

    @Test
    void shouldCreateTranslatedPageableInstance() {
        // Arrange
        Map<String, String> translationMap = Map.of("eventDate", "event_date");
        Pageable originalPageable = PageRequest.of(2, 15, Sort.by("eventDate"));

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        assertThat(translated).isInstanceOf(TranslatedPageable.class);
        assertThat(translated.isPaged()).isTrue();
        assertThat(translated.isUnpaged()).isFalse();
    }

    @Test
    void shouldSupportPageableNavigationMethods() {
        // Arrange
        Map<String, String> translationMap = Map.of("eventDate", "event_date");
        Pageable originalPageable = PageRequest.of(1, 10, Sort.by("eventDate"));

        // Act
        Pageable translated = TranslatedPageable.translate(originalPageable, translationMap);

        // Assert
        Pageable next = translated.next();
        assertThat(next.getPageNumber()).isEqualTo(2);
        assertThat(next.getPageSize()).isEqualTo(10);

        Pageable previous = translated.previousOrFirst();
        assertThat(previous.getPageNumber()).isEqualTo(0);

        Pageable first = translated.first();
        assertThat(first.getPageNumber()).isEqualTo(0);

        assertThat(translated.hasPrevious()).isTrue();
    }
}
