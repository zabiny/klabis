package club.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static club.klabis.events.domain.EventConditions.*;
import static club.klabis.events.domain.EventTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests for Competition class")
class CompetitionTest {

    @Nested
    @DisplayName("newEvent factory method tests")
    class NewEventFactoryTests {

        @Test
        @DisplayName("Should successfully create competition with name and date")
        void shouldCreateCompetitionWithNameAndDate() {
            // Arrange
            String name = "Test Competition";
            LocalDate eventDate = DEFAULT_EVENT_DATE;
            Set<Competition.Category> categories = Set.of(CATEGORY_D12);

            // Act
            Competition competition = Competition.newEvent(name, eventDate, categories);

            // Assert
            assertThat(competition)
                    .isNotNull()
                    .has(hasName(name))
                    .has(hasEventDate(eventDate));
        }

        @Test
        @DisplayName("Should successfully create competition with categories")
        void shouldCreateCompetitionWithCategories() {
            // Arrange
            Set<Competition.Category> categories = Set.of(CATEGORY_D12, CATEGORY_H21, CATEGORY_D21);

            // Act
            Competition competition = Competition.newEvent("Test", DEFAULT_EVENT_DATE, categories);

            // Assert
            assertThat(competition)
                    .has(hasCategories(CATEGORY_D12, CATEGORY_H21, CATEGORY_D21))
                    .has(hasCategoryCount(3));
        }
    }

    @Nested
    @DisplayName("setCategories method tests")
    class SetCategoriesTests {

        @Test
        @DisplayName("Should successfully set categories when no registrations exist")
        void shouldSetCategoriesWhenNoRegistrationsExist() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());
            Set<Competition.Category> newCategories = Set.of(CATEGORY_D21, CATEGORY_H21, CATEGORY_D35);

            // Act
            competition.setCategories(newCategories);

            // Assert
            assertThat(competition)
                    .has(hasCategories(CATEGORY_D21, CATEGORY_H21, CATEGORY_D35))
                    .has(hasCategoryCount(3));
        }

        @Test
        @DisplayName("Should allow removing categories that have no registrations")
        void shouldAllowRemovingCategoriesWithoutRegistrations() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(),
                    CATEGORY_H21.name(),
                    CATEGORY_D21.name());
            // Register member only in D12
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));

            // Act - Remove H21 and D21 (no registrations), keep D12
            Set<Competition.Category> newCategories = Set.of(CATEGORY_D12);
            competition.setCategories(newCategories);

            // Assert
            assertThat(competition)
                    .has(hasCategories(CATEGORY_D12))
                    .has(hasCategoryCount(1));
        }

        @Test
        @DisplayName("Should throw EventException with BLOCKED_CATEGORIES type and include category names when removing categories with registrations")
        void shouldThrowExceptionWhenRemovingCategoriesWithRegistrations() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));

            // Act & Assert - Try to remove D12 (has registration)
            Set<Competition.Category> newCategories = Set.of(CATEGORY_H21); // Removing D12
            assertThatThrownBy(() -> competition.setCategories(newCategories))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot remove categories")
                    .hasMessageContaining("D12")
                    .extracting(ex -> ((EventException) ex).getType())
                    .isEqualTo(EventException.Type.BLOCKED_CATEGORIES);
        }

        @Test
        @DisplayName("Should allow adding new categories without removing existing ones")
        void shouldAllowAddingNewCategories() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));

            // Act - Add D21 and D35, keep existing
            Set<Competition.Category> newCategories = Set.of(CATEGORY_D12, CATEGORY_H21, CATEGORY_D21, CATEGORY_D35);
            competition.setCategories(newCategories);

            // Assert
            assertThat(competition)
                    .has(hasCategories(CATEGORY_D12, CATEGORY_H21, CATEGORY_D21, CATEGORY_D35))
                    .has(hasCategoryCount(4));
        }

        @Test
        @DisplayName("Should clear and replace all categories when possible")
        void shouldClearAndReplaceAllCategories() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());
            // No registrations

            // Act - Complete replacement
            Set<Competition.Category> newCategories = Set.of(CATEGORY_D21, CATEGORY_D35);
            competition.setCategories(newCategories);

            // Assert
            assertThat(competition)
                    .has(hasCategories(CATEGORY_D21, CATEGORY_D35))
                    .has(hasCategoryCount(2))
                    .has(doesNotHaveCategory(CATEGORY_D12))
                    .has(doesNotHaveCategory(CATEGORY_H21));
        }

        @Test
        @DisplayName("Should allow removing multiple categories when none have registrations")
        void shouldAllowRemovingMultipleCategoriesWithoutRegistrations() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(),
                    CATEGORY_H21.name(),
                    CATEGORY_D21.name(),
                    CATEGORY_D35.name());
            // Register in only one category
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));

            // Act - Remove H21, D21, D35, keep D12
            Set<Competition.Category> newCategories = Set.of(CATEGORY_D12);
            competition.setCategories(newCategories);

            // Assert
            assertThat(competition)
                    .has(hasCategories(CATEGORY_D12))
                    .has(hasCategoryCount(1));
        }

        @Test
        @DisplayName("Should fail when trying to remove ANY category that has registrations among multiple removals")
        void shouldFailWhenRemovingAnyCategoryWithRegistrations() {
            // Arrange
            Competition competition = createCompetitionWithCategories(
                    CATEGORY_D12.name(), CATEGORY_H21.name(), CATEGORY_D21.name());
            // Register in D12 and H21
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));
            competition.registerMember(MEMBER_2, formForCategory(CATEGORY_H21));

            // Act & Assert - Try to keep only D21 (removing D12 and H21 which have registrations)
            Set<Competition.Category> newCategories = Set.of(CATEGORY_D21);
            assertThatThrownBy(() -> competition.setCategories(newCategories))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("D12")
                    .hasMessageContaining("H21");
        }
    }

    @Nested
    @DisplayName("getRegistrationsForCategory method tests")
    class GetRegistrationsForCategoryTests {

        @Test
        @DisplayName("Should return empty set when no registrations exist for category")
        void shouldReturnEmptySetWhenNoCategoryRegistrations() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());

            // Act
            Set<Registration> registrations = competition.getRegistrationsForCategory(CATEGORY_D12);

            // Assert
            assertThat(registrations).isEmpty();
        }

        @Test
        @DisplayName("Should return registrations for specific category only")
        void shouldReturnRegistrationsForSpecificCategory() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));
            competition.registerMember(MEMBER_2, formForCategory(CATEGORY_D12));
            competition.registerMember(MEMBER_3, formForCategory(CATEGORY_H21));

            // Act
            Set<Registration> d12Registrations = competition.getRegistrationsForCategory(CATEGORY_D12);

            // Assert
            assertThat(d12Registrations)
                    .hasSize(2)
                    .extracting(Registration::getMemberId)
                    .containsExactlyInAnyOrder(MEMBER_1, MEMBER_2);
        }

        @Test
        @DisplayName("Should NOT return registrations from other categories")
        void shouldNotReturnRegistrationsFromOtherCategories() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));
            competition.registerMember(MEMBER_2, formForCategory(CATEGORY_H21));

            // Act
            Set<Registration> d12Registrations = competition.getRegistrationsForCategory(CATEGORY_D12);

            // Assert
            assertThat(d12Registrations)
                    .hasSize(1)
                    .extracting(Registration::getMemberId)
                    .containsExactly(MEMBER_1)
                    .doesNotContain(MEMBER_2);
        }

        @Test
        @DisplayName("Should return empty set when category doesn't exist in competition")
        void shouldReturnEmptySetWhenCategoryNotFound() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name());
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));

            // Act - Query for category not in competition
            Set<Registration> registrations = competition.getRegistrationsForCategory(CATEGORY_H21);

            // Assert
            assertThat(registrations).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCategories method tests")
    class GetCategoriesTests {

        @Test
        @DisplayName("Should return defensive copy (modifications don't affect internal state)")
        void shouldReturnDefensiveCopy() {
            // Arrange
            Competition competition = createCompetitionWithCategories(CATEGORY_D12.name(), CATEGORY_H21.name());

            // Act
            Set<Competition.Category> categories = competition.getCategories();

            // Assert - Returned set should be unmodifiable
            assertThatThrownBy(() -> categories.add(CATEGORY_D21))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
