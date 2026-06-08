package com.klabis.membershipfees.domain;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeeYearPublication domain tests")
class FeeYearPublicationTest {

    private static final int YEAR = 2026;
    private static final LocalDate DEADLINE = LocalDate.of(2026, 3, 31);
    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));

    private MembershipFeeLevel buildLevel(String name) {
        return MembershipFeeLevel.create(name, YEARLY_FEE, List.of());
    }

    @Nested
    @DisplayName("publish() factory method")
    class Publish {

        @Test
        @DisplayName("should create publication with year, deadline, and groups for each level")
        void shouldCreatePublicationWithYearAndDeadline() {
            MembershipFeeLevel level = buildLevel("Dospělý");

            var result = FeeYearPublication.publish(YEAR, DEADLINE, List.of(level));

            assertThat(result.publication().getId()).isNotNull();
            assertThat(result.publication().getYear()).isEqualTo(YEAR);
            assertThat(result.publication().getVotingDeadline()).isEqualTo(DEADLINE);
            assertThat(result.publication().getPublishedGroupIds()).hasSize(1);
        }

        @Test
        @DisplayName("should create one MembershipFeeGroup snapshot per level")
        void shouldCreateGroupSnapshotsForEachLevel() {
            MembershipFeeLevel level1 = buildLevel("Dospělý");
            MembershipFeeLevel level2 = buildLevel("Mládež");

            var result = FeeYearPublication.publish(YEAR, DEADLINE, List.of(level1, level2));

            assertThat(result.publication().getPublishedGroupIds()).hasSize(2);
            assertThat(result.groups()).hasSize(2);
        }

        @Test
        @DisplayName("should expose created groups via groups()")
        void shouldExposeCreatedGroups() {
            MembershipFeeLevel level = buildLevel("Závodník");

            var result = FeeYearPublication.publish(YEAR, DEADLINE, List.of(level));

            assertThat(result.groups()).hasSize(1);
            MembershipFeeGroup group = result.groups().get(0);
            assertThat(group.getName()).isEqualTo("Závodník");
            assertThat(group.getYear()).isEqualTo(YEAR);
            assertThat(group.getYearlyFeeSnapshot()).isEqualTo(YEARLY_FEE);
        }

        @Test
        @DisplayName("should set deadlineProcessedAt to null at creation")
        void shouldHaveNullDeadlineProcessedAt() {
            var result = FeeYearPublication.publish(YEAR, DEADLINE, List.of(buildLevel("Dospělý")));

            assertThat(result.publication().getDeadlineProcessedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("isClosed()")
    class IsClosed {

        @Test
        @DisplayName("should return true when today is after voting deadline")
        void shouldReturnTrueWhenAfterDeadline() {
            var publication = FeeYearPublication.publish(YEAR, DEADLINE, List.of(buildLevel("Dospělý"))).publication();
            LocalDate dayAfterDeadline = DEADLINE.plusDays(1);

            assertThat(publication.isClosed(dayAfterDeadline)).isTrue();
        }

        @Test
        @DisplayName("should return false when today is on the voting deadline")
        void shouldReturnFalseOnDeadline() {
            var publication = FeeYearPublication.publish(YEAR, DEADLINE, List.of(buildLevel("Dospělý"))).publication();

            assertThat(publication.isClosed(DEADLINE)).isFalse();
        }

        @Test
        @DisplayName("should return false when today is before voting deadline")
        void shouldReturnFalseBeforeDeadline() {
            var publication = FeeYearPublication.publish(YEAR, DEADLINE, List.of(buildLevel("Dospělý"))).publication();
            LocalDate dayBeforeDeadline = DEADLINE.minusDays(1);

            assertThat(publication.isClosed(dayBeforeDeadline)).isFalse();
        }
    }

    @Nested
    @DisplayName("markProcessed()")
    class MarkProcessed {

        @Test
        @DisplayName("should set deadlineProcessedAt to the given instant")
        void shouldSetDeadlineProcessedAt() {
            var publication = FeeYearPublication.publish(YEAR, DEADLINE, List.of(buildLevel("Dospělý"))).publication();
            Instant processedAt = Instant.parse("2026-04-01T10:00:00Z");

            publication.markProcessed(processedAt);

            assertThat(publication.getDeadlineProcessedAt()).isEqualTo(processedAt);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class Reconstruct {

        @Test
        @DisplayName("should reconstruct publication with all fields")
        void shouldReconstructWithAllFields() {
            FeeYearPublicationId id = new FeeYearPublicationId(UUID.randomUUID());
            MembershipFeeGroupId groupId = new MembershipFeeGroupId(UUID.randomUUID());
            Instant processedAt = Instant.parse("2026-04-01T10:00:00Z");

            FeeYearPublication publication = FeeYearPublication.reconstruct(
                    id, YEAR, DEADLINE, processedAt, List.of(groupId));

            assertThat(publication.getId()).isEqualTo(id);
            assertThat(publication.getYear()).isEqualTo(YEAR);
            assertThat(publication.getVotingDeadline()).isEqualTo(DEADLINE);
            assertThat(publication.getDeadlineProcessedAt()).isEqualTo(processedAt);
            assertThat(publication.getPublishedGroupIds()).containsExactly(groupId);
        }
    }
}
