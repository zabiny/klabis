package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import com.klabis.membershipfees.domain.MembershipFeeLevelRepository;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MembershipFeeLevel JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class MembershipFeeLevelPersistenceTest {

    private static final EventTypeReference EVENT_TYPE_A = EventTypeReference.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final EventTypeReference EVENT_TYPE_B = EventTypeReference.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));

    @Autowired
    private MembershipFeeLevelRepository repository;

    @Nested
    @DisplayName("save() and findById() — round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should save and retrieve level with name and yearly fee")
        void shouldSaveAndRetrieveLevelWithBasicFields() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Dospělý", YEARLY_FEE, List.of());

            MembershipFeeLevel saved = repository.save(level);
            Optional<MembershipFeeLevel> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            MembershipFeeLevel retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Dospělý");
            assertThat(retrieved.getYearlyFee()).isEqualTo(YEARLY_FEE);
            assertThat(retrieved.getRules()).isEmpty();
        }

        @Test
        @DisplayName("should persist audit metadata after save")
        void shouldPersistAuditMetadata() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Audit Level", YEARLY_FEE, List.of());

            MembershipFeeLevel saved = repository.save(level);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should save and retrieve level with percentage payment rule")
        void shouldSaveAndRetrieveLevelWithPercentageRule() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of(rule));

            MembershipFeeLevel saved = repository.save(level);
            Optional<MembershipFeeLevel> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            List<MembershipPaymentRule> rules = found.get().getRules();
            assertThat(rules).hasSize(1);
            MembershipPaymentRule retrievedRule = rules.get(0);
            assertThat(retrievedRule.eventTypeId()).isEqualTo(EVENT_TYPE_A);
            assertThat(retrievedRule.rankingShortName()).isEqualTo("A");
            assertThat(retrievedRule.value()).isInstanceOf(MembershipPaymentRule.RuleValue.Percentage.class);
            assertThat(((MembershipPaymentRule.RuleValue.Percentage) retrievedRule.value()).percent()).isEqualTo(50);
        }

        @Test
        @DisplayName("should save and retrieve level with fixed amount payment rule")
        void shouldSaveAndRetrieveLevelWithFixedAmountRule() {
            Money surchargeAmount = Money.ofCzk(new BigDecimal("200.00"));
            MembershipPaymentRule rule = MembershipPaymentRule.fixedAmount(EVENT_TYPE_B, "LOB", surchargeAmount);
            MembershipFeeLevel level = MembershipFeeLevel.create("Mládež", YEARLY_FEE, List.of(rule));

            MembershipFeeLevel saved = repository.save(level);
            Optional<MembershipFeeLevel> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            List<MembershipPaymentRule> rules = found.get().getRules();
            assertThat(rules).hasSize(1);
            MembershipPaymentRule retrievedRule = rules.get(0);
            assertThat(retrievedRule.value()).isInstanceOf(MembershipPaymentRule.RuleValue.FixedAmount.class);
            assertThat(((MembershipPaymentRule.RuleValue.FixedAmount) retrievedRule.value()).amount())
                    .isEqualTo(surchargeAmount);
        }

        @Test
        @DisplayName("should save and retrieve level with multiple rules")
        void shouldSaveAndRetrieveLevelWithMultipleRules() {
            MembershipPaymentRule rule1 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule rule2 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "B", 30);
            MembershipPaymentRule rule3 = MembershipPaymentRule.fixedAmount(EVENT_TYPE_B, "LOB",
                    Money.ofCzk(new BigDecimal("100")));
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of(rule1, rule2, rule3));

            MembershipFeeLevel saved = repository.save(level);
            Optional<MembershipFeeLevel> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getRules()).hasSize(3);
        }

        @Test
        @DisplayName("should return empty when level not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<MembershipFeeLevel> found = repository.findById(new MembershipFeeLevelId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should persist updated name after second save")
        void shouldPersistUpdatedName() {
            MembershipFeeLevel level = repository.save(
                    MembershipFeeLevel.create("Original Name", YEARLY_FEE, List.of()));
            level.editName("Updated Name");

            repository.save(level);
            MembershipFeeLevel retrieved = repository.findById(level.getId()).orElseThrow();

            assertThat(retrieved.getName()).isEqualTo("Updated Name");
        }
    }

    @Nested
    @DisplayName("findAll()")
    @CleanupTestData
    class FindAll {

        @Test
        @DisplayName("should return all saved levels")
        void shouldReturnAllLevels() {
            repository.save(MembershipFeeLevel.create("Level A", YEARLY_FEE, List.of()));
            repository.save(MembershipFeeLevel.create("Level B", Money.ofCzk(new BigDecimal("800")), List.of()));

            List<MembershipFeeLevel> all = repository.findAll();

            assertThat(all).hasSize(2);
            assertThat(all).extracting(MembershipFeeLevel::getName)
                    .containsExactlyInAnyOrder("Level A", "Level B");
        }

        @Test
        @DisplayName("should return empty list when no levels exist")
        void shouldReturnEmptyList() {
            assertThat(repository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should delete level so it can no longer be found")
        void shouldDeleteLevel() {
            MembershipFeeLevel level = repository.save(
                    MembershipFeeLevel.create("To Delete", YEARLY_FEE, List.of()));

            repository.delete(level.getId());

            assertThat(repository.findById(level.getId())).isEmpty();
        }
    }
}
