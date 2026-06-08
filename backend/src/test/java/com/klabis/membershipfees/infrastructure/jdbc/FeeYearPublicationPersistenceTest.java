package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.events.EventTypeId;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeeYearPublication JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class FeeYearPublicationPersistenceTest {

    private static final EventTypeId EVENT_TYPE = EventTypeId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));
    private static final LocalDate DEADLINE = LocalDate.of(2026, 3, 31);

    @Autowired
    private FeeYearPublicationRepository publicationRepository;

    @Autowired
    private MembershipFeeGroupRepository groupRepository;

    @Autowired
    private MembershipFeeLevelRepository levelRepository;

    private MembershipFeeLevel savedLevel(String name) {
        return levelRepository.save(MembershipFeeLevel.create(name, YEARLY_FEE, List.of()));
    }

    private FeeYearPublication publishAndSaveGroups(int year, List<MembershipFeeLevel> levels) {
        var result = FeeYearPublication.publish(year, DEADLINE, levels);
        for (MembershipFeeGroup group : result.groups()) {
            groupRepository.save(group);
        }
        return result.publication();
    }

    @Nested
    @DisplayName("FeeYearPublication save() and findById() — round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should save and retrieve publication with year and deadline")
        void shouldSaveAndRetrieveBasicFields() {
            MembershipFeeLevel level = savedLevel("Dospělý");
            FeeYearPublication publication = publishAndSaveGroups(2026, List.of(level));

            FeeYearPublication saved = publicationRepository.save(publication);
            Optional<FeeYearPublication> found = publicationRepository.findById(saved.getId());

            assertThat(found).isPresent();
            FeeYearPublication retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getYear()).isEqualTo(2026);
            assertThat(retrieved.getVotingDeadline()).isEqualTo(DEADLINE);
            assertThat(retrieved.getDeadlineProcessedAt()).isNull();
        }

        @Test
        @DisplayName("should persist publishedGroupIds correctly")
        void shouldPersistGroupIds() {
            MembershipFeeLevel level1 = savedLevel("Dospělý");
            MembershipFeeLevel level2 = savedLevel("Mládež");
            FeeYearPublication publication = publishAndSaveGroups(2026, List.of(level1, level2));

            FeeYearPublication saved = publicationRepository.save(publication);
            FeeYearPublication found = publicationRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getPublishedGroupIds()).hasSize(2);
        }

        @Test
        @DisplayName("should persist deadlineProcessedAt after markProcessed()")
        void shouldPersistDeadlineProcessedAt() {
            MembershipFeeLevel level = savedLevel("Dospělý");
            FeeYearPublication publication = publishAndSaveGroups(2026, List.of(level));
            FeeYearPublication saved = publicationRepository.save(publication);
            Instant processedAt = Instant.parse("2026-04-01T10:00:00Z");
            saved.markProcessed(processedAt);

            publicationRepository.save(saved);
            FeeYearPublication found = publicationRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getDeadlineProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return empty when publication not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<FeeYearPublication> found =
                    publicationRepository.findById(new FeeYearPublicationId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByYear()")
    class FindByYear {

        @Test
        @DisplayName("should find publication by year")
        void shouldFindByYear() {
            MembershipFeeLevel level = savedLevel("Dospělý");
            FeeYearPublication publication = publishAndSaveGroups(2027, List.of(level));
            publicationRepository.save(publication);

            Optional<FeeYearPublication> found = publicationRepository.findByYear(2027);

            assertThat(found).isPresent();
            assertThat(found.get().getYear()).isEqualTo(2027);
        }

        @Test
        @DisplayName("should return empty when no publication for year")
        void shouldReturnEmptyForMissingYear() {
            assertThat(publicationRepository.findByYear(9999)).isEmpty();
        }
    }

    @Nested
    @DisplayName("MembershipFeeGroup save() and findById() — round-trip")
    class GroupSaveAndFindById {

        @Test
        @DisplayName("should save and retrieve group with snapshot fields")
        void shouldSaveAndRetrieveGroupSnapshot() {
            MembershipFeeLevel level = savedLevel("Závodník");
            MembershipFeeLevelId sourceLevelId = level.getId();
            MembershipPaymentRuleSnapshot rule = new MembershipPaymentRuleSnapshot(
                    EVENT_TYPE, "A", new MembershipPaymentRule.RuleValue.Percentage(50));
            MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                    sourceLevelId, "Závodník", 2026, YEARLY_FEE, List.of(rule));

            MembershipFeeGroup saved = groupRepository.save(group);
            Optional<MembershipFeeGroup> found = groupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            MembershipFeeGroup retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getSourceLevelId()).isEqualTo(sourceLevelId);
            assertThat(retrieved.getName()).isEqualTo("Závodník");
            assertThat(retrieved.getYear()).isEqualTo(2026);
            assertThat(retrieved.getYearlyFeeSnapshot()).isEqualTo(YEARLY_FEE);
            assertThat(retrieved.getStatus()).isEqualTo(PublishedLevelStatus.EDITABLE);
        }

        @Test
        @DisplayName("should persist rule snapshot correctly")
        void shouldPersistRuleSnapshot() {
            MembershipFeeLevel level = savedLevel("Závodník2");
            MembershipPaymentRuleSnapshot rule = new MembershipPaymentRuleSnapshot(
                    EVENT_TYPE, "LOB", new MembershipPaymentRule.RuleValue.FixedSurcharge(
                    Money.ofCzk(new BigDecimal("200.00"))));
            MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                    level.getId(), "Závodník2", 2026, YEARLY_FEE, List.of(rule));

            MembershipFeeGroup saved = groupRepository.save(group);
            MembershipFeeGroup found = groupRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getRulesSnapshot()).hasSize(1);
            MembershipPaymentRuleSnapshot retrieved = found.getRulesSnapshot().get(0);
            assertThat(retrieved.eventTypeId()).isEqualTo(EVENT_TYPE);
            assertThat(retrieved.rankingShortName()).isEqualTo("LOB");
            assertThat(retrieved.value()).isInstanceOf(MembershipPaymentRule.RuleValue.FixedSurcharge.class);
        }

        @Test
        @DisplayName("should persist FROZEN status after freeze()")
        void shouldPersistFrozenStatus() {
            MembershipFeeLevel level = savedLevel("DospělýFreeze");
            MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                    level.getId(), "DospělýFreeze", 2026, YEARLY_FEE, List.of());
            MembershipFeeGroup saved = groupRepository.save(group);
            saved.freeze();

            groupRepository.save(saved);
            MembershipFeeGroup found = groupRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getStatus()).isEqualTo(PublishedLevelStatus.FROZEN);
        }

        @Test
        @DisplayName("should find groups by year")
        void shouldFindGroupsByYear() {
            MembershipFeeLevel level1 = savedLevel("Dospělý2026A");
            MembershipFeeLevel level2 = savedLevel("Mládež2026A");
            MembershipFeeLevel level3 = savedLevel("JinýRok2025A");
            groupRepository.save(MembershipFeeGroup.createSnapshot(level1.getId(), "Dospělý", 2026, YEARLY_FEE, List.of()));
            groupRepository.save(MembershipFeeGroup.createSnapshot(level2.getId(), "Mládež", 2026, YEARLY_FEE, List.of()));
            groupRepository.save(MembershipFeeGroup.createSnapshot(level3.getId(), "Jiný rok", 2025, YEARLY_FEE, List.of()));

            List<MembershipFeeGroup> groups = groupRepository.findByYear(2026);

            assertThat(groups).hasSize(2);
        }
    }
}
