package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.membershipfees.domain.YearlyFeeChargeMarkerRepository;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YearlyFeeChargeMarker JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class YearlyFeeChargeMarkerPersistenceTest {

    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_B = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Autowired
    private YearlyFeeChargeMarkerRepository markerRepository;

    @Nested
    @DisplayName("existsByMemberIdAndYear()")
    class ExistsByMemberIdAndYear {

        @Test
        @DisplayName("should return false when no marker exists for member and year")
        void shouldReturnFalseWhenNoMarkerExists() {
            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2026)).isFalse();
        }

        @Test
        @DisplayName("should return true after marking charged")
        void shouldReturnTrueAfterMarking() {
            markerRepository.markCharged(MEMBER_A, 2026);

            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2026)).isTrue();
        }

        @Test
        @DisplayName("should return false for different year with same member")
        void shouldReturnFalseForDifferentYear() {
            markerRepository.markCharged(MEMBER_A, 2026);

            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2025)).isFalse();
            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2027)).isFalse();
        }

        @Test
        @DisplayName("should return false for different member with same year")
        void shouldReturnFalseForDifferentMember() {
            markerRepository.markCharged(MEMBER_A, 2026);

            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_B, 2026)).isFalse();
        }
    }

    @Nested
    @DisplayName("markCharged() idempotence")
    class MarkChargedIdempotence {

        @Test
        @DisplayName("should be idempotent — marking twice does not throw")
        void shouldBeIdempotent() {
            markerRepository.markCharged(MEMBER_A, 2026);
            markerRepository.markCharged(MEMBER_A, 2026);

            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2026)).isTrue();
        }

        @Test
        @DisplayName("should allow marking same member for different years independently")
        void shouldAllowDifferentYearsForSameMember() {
            markerRepository.markCharged(MEMBER_A, 2025);
            markerRepository.markCharged(MEMBER_A, 2026);

            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2025)).isTrue();
            assertThat(markerRepository.existsByMemberIdAndYear(MEMBER_A, 2026)).isTrue();
        }
    }
}
