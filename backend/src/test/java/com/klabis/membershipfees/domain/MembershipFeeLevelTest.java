package com.klabis.membershipfees.domain;

import com.klabis.events.EventTypeId;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MembershipFeeLevel domain unit tests")
class MembershipFeeLevelTest {

    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));
    private static final Money OTHER_FEE = Money.ofCzk(new BigDecimal("800.00"));

    private static final EventTypeId EVENT_TYPE_A = EventTypeId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final EventTypeId EVENT_TYPE_B = EventTypeId.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Nested
    @DisplayName("MembershipFeeLevelId")
    class IdTests {

        @Test
        @DisplayName("should wrap UUID value")
        void shouldWrapUuidValue() {
            UUID uuid = UUID.randomUUID();
            MembershipFeeLevelId id = new MembershipFeeLevelId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("should expose uuid() convenience accessor")
        void shouldExposeUuidAccessor() {
            UUID uuid = UUID.randomUUID();
            MembershipFeeLevelId id = new MembershipFeeLevelId(uuid);

            assertThat(id.uuid()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("two IDs with the same UUID should be equal")
        void shouldBeEqualWhenSameUuid() {
            UUID uuid = UUID.randomUUID();
            assertThat(new MembershipFeeLevelId(uuid)).isEqualTo(new MembershipFeeLevelId(uuid));
        }

        @Test
        @DisplayName("two IDs with different UUIDs should not be equal")
        void shouldNotBeEqualWhenDifferentUuid() {
            assertThat(new MembershipFeeLevelId(UUID.randomUUID()))
                    .isNotEqualTo(new MembershipFeeLevelId(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("MembershipFeeLevel.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create level with name and yearly fee")
        void shouldCreateLevelWithNameAndYearlyFee() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Dospělý", YEARLY_FEE, List.of());

            assertThat(level.getId()).isNotNull();
            assertThat(level.getName()).isEqualTo("Dospělý");
            assertThat(level.getYearlyFee()).isEqualTo(YEARLY_FEE);
        }

        @Test
        @DisplayName("should create level with payment rules")
        void shouldCreateLevelWithRules() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of(rule));

            assertThat(level.getRules()).hasSize(1);
            assertThat(level.getRules()).contains(rule);
        }

        @Test
        @DisplayName("should generate unique IDs for each created level")
        void shouldGenerateUniqueIds() {
            MembershipFeeLevel level1 = MembershipFeeLevel.create("Level A", YEARLY_FEE, List.of());
            MembershipFeeLevel level2 = MembershipFeeLevel.create("Level B", YEARLY_FEE, List.of());

            assertThat(level1.getId()).isNotEqualTo(level2.getId());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> MembershipFeeLevel.create("", YEARLY_FEE, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> MembershipFeeLevel.create(null, YEARLY_FEE, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null yearly fee")
        void shouldRejectNullYearlyFee() {
            assertThatThrownBy(() -> MembershipFeeLevel.create("Dospělý", null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("editName()")
    class EditName {

        @Test
        @DisplayName("should update the name")
        void shouldUpdateName() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Old Name", YEARLY_FEE, List.of());

            level.editName("New Name");

            assertThat(level.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Some Name", YEARLY_FEE, List.of());

            assertThatThrownBy(() -> level.editName(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Some Name", YEARLY_FEE, List.of());

            assertThatThrownBy(() -> level.editName(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("editYearlyFee()")
    class EditYearlyFee {

        @Test
        @DisplayName("should update the yearly fee")
        void shouldUpdateYearlyFee() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Dospělý", YEARLY_FEE, List.of());

            level.editYearlyFee(OTHER_FEE);

            assertThat(level.getYearlyFee()).isEqualTo(OTHER_FEE);
        }

        @Test
        @DisplayName("should reject null fee")
        void shouldRejectNullFee() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Dospělý", YEARLY_FEE, List.of());

            assertThatThrownBy(() -> level.editYearlyFee(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("replaceRules()")
    class ReplaceRules {

        @Test
        @DisplayName("should replace all existing rules with new set")
        void shouldReplaceRules() {
            MembershipPaymentRule originalRule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of(originalRule));

            MembershipPaymentRule newRule1 = MembershipPaymentRule.percentage(EVENT_TYPE_B, "B", 30);
            MembershipPaymentRule newRule2 = MembershipPaymentRule.fixedSurcharge(EVENT_TYPE_A, "A",
                    Money.ofCzk(new BigDecimal("200")));

            level.replaceRules(List.of(newRule1, newRule2));

            assertThat(level.getRules()).hasSize(2);
            assertThat(level.getRules()).containsExactlyInAnyOrder(newRule1, newRule2);
        }

        @Test
        @DisplayName("should allow replacing with empty set")
        void shouldAllowEmptyReplacement() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of(rule));

            level.replaceRules(List.of());

            assertThat(level.getRules()).isEmpty();
        }

        @Test
        @DisplayName("should reject duplicate rule key in replacement set")
        void shouldRejectDuplicateKeyInReplacement() {
            MembershipPaymentRule rule1 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule rule2 = MembershipPaymentRule.fixedSurcharge(EVENT_TYPE_A, "A",
                    Money.ofCzk(new BigDecimal("200")));
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of());

            assertThatThrownBy(() -> level.replaceRules(List.of(rule1, rule2)))
                    .isInstanceOf(DuplicatePaymentRuleException.class);
        }
    }

    @Nested
    @DisplayName("addRule()")
    class AddRule {

        @Test
        @DisplayName("should add a new rule to the level")
        void shouldAddRule() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of());
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            level.addRule(rule);

            assertThat(level.getRules()).hasSize(1);
            assertThat(level.getRules()).contains(rule);
        }

        @Test
        @DisplayName("should allow adding rules for different event type + ranking combinations")
        void shouldAllowDifferentCombinations() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of());

            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "B", 30));
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_B, "A", 70));

            assertThat(level.getRules()).hasSize(3);
        }

        @Test
        @DisplayName("should reject duplicate (eventTypeId, rankingShortName) combination")
        void shouldRejectDuplicateRule() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of());
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            assertThatThrownBy(() -> level.addRule(
                    MembershipPaymentRule.fixedSurcharge(EVENT_TYPE_A, "A", Money.ofCzk(new BigDecimal("200")))))
                    .isInstanceOf(DuplicatePaymentRuleException.class);
        }

        @Test
        @DisplayName("should reject null rule")
        void shouldRejectNullRule() {
            MembershipFeeLevel level = MembershipFeeLevel.create("Závodník", YEARLY_FEE, List.of());

            assertThatThrownBy(() -> level.addRule(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class Reconstruct {

        @Test
        @DisplayName("should reconstruct level with all fields")
        void shouldReconstructWithAllFields() {
            MembershipFeeLevelId id = new MembershipFeeLevelId(UUID.randomUUID());
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            MembershipFeeLevel level = MembershipFeeLevel.reconstruct(id, "Dospělý", YEARLY_FEE, List.of(rule), null);

            assertThat(level.getId()).isEqualTo(id);
            assertThat(level.getName()).isEqualTo("Dospělý");
            assertThat(level.getYearlyFee()).isEqualTo(YEARLY_FEE);
            assertThat(level.getRules()).containsExactly(rule);
        }
    }
}
