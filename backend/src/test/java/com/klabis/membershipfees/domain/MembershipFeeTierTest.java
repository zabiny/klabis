package com.klabis.membershipfees.domain;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MembershipFeeTier domain unit tests")
class MembershipFeeTierTest {

    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));
    private static final Money OTHER_FEE = Money.ofCzk(new BigDecimal("800.00"));

    private static final EventTypeReference EVENT_TYPE_A = EventTypeReference.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final EventTypeReference EVENT_TYPE_B = EventTypeReference.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Nested
    @DisplayName("MembershipFeeTierId")
    class IdTests {

        @Test
        @DisplayName("should wrap UUID value")
        void shouldWrapUuidValue() {
            UUID uuid = UUID.randomUUID();
            MembershipFeeTierId id = new MembershipFeeTierId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("two IDs with the same UUID should be equal")
        void shouldBeEqualWhenSameUuid() {
            UUID uuid = UUID.randomUUID();
            assertThat(new MembershipFeeTierId(uuid)).isEqualTo(new MembershipFeeTierId(uuid));
        }

        @Test
        @DisplayName("two IDs with different UUIDs should not be equal")
        void shouldNotBeEqualWhenDifferentUuid() {
            assertThat(new MembershipFeeTierId(UUID.randomUUID()))
                    .isNotEqualTo(new MembershipFeeTierId(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("MembershipFeeTier.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create level with name and yearly fee")
        void shouldCreateLevelWithNameAndYearlyFee() {
            MembershipFeeTier level = MembershipFeeTier.create("Dospělý", YEARLY_FEE, List.of());

            assertThat(level.getId()).isNotNull();
            assertThat(level.getName()).isEqualTo("Dospělý");
            assertThat(level.getYearlyFee()).isEqualTo(YEARLY_FEE);
        }

        @Test
        @DisplayName("should create level with payment rules")
        void shouldCreateLevelWithRules() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of(rule));

            assertThat(level.getRules()).hasSize(1);
            assertThat(level.getRules()).contains(rule);
        }

        @Test
        @DisplayName("should generate unique IDs for each created level")
        void shouldGenerateUniqueIds() {
            MembershipFeeTier level1 = MembershipFeeTier.create("Level A", YEARLY_FEE, List.of());
            MembershipFeeTier level2 = MembershipFeeTier.create("Level B", YEARLY_FEE, List.of());

            assertThat(level1.getId()).isNotEqualTo(level2.getId());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> MembershipFeeTier.create("", YEARLY_FEE, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> MembershipFeeTier.create(null, YEARLY_FEE, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null yearly fee")
        void shouldRejectNullYearlyFee() {
            assertThatThrownBy(() -> MembershipFeeTier.create("Dospělý", null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("editName()")
    class EditName {

        @Test
        @DisplayName("should update the name")
        void shouldUpdateName() {
            MembershipFeeTier level = MembershipFeeTier.create("Old Name", YEARLY_FEE, List.of());

            level.editName("New Name");

            assertThat(level.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            MembershipFeeTier level = MembershipFeeTier.create("Some Name", YEARLY_FEE, List.of());

            assertThatThrownBy(() -> level.editName(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            MembershipFeeTier level = MembershipFeeTier.create("Some Name", YEARLY_FEE, List.of());

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
            MembershipFeeTier level = MembershipFeeTier.create("Dospělý", YEARLY_FEE, List.of());

            level.editYearlyFee(OTHER_FEE);

            assertThat(level.getYearlyFee()).isEqualTo(OTHER_FEE);
        }

        @Test
        @DisplayName("should reject null fee")
        void shouldRejectNullFee() {
            MembershipFeeTier level = MembershipFeeTier.create("Dospělý", YEARLY_FEE, List.of());

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
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of(originalRule));

            MembershipPaymentRule newRule1 = MembershipPaymentRule.percentage(EVENT_TYPE_B, "B", 30);
            MembershipPaymentRule newRule2 = MembershipPaymentRule.fixedAmount(EVENT_TYPE_A, "A",
                    Money.ofCzk(new BigDecimal("200")));

            level.replaceRules(List.of(newRule1, newRule2));

            assertThat(level.getRules()).hasSize(2);
            assertThat(level.getRules()).containsExactlyInAnyOrder(newRule1, newRule2);
        }

        @Test
        @DisplayName("should allow replacing with empty set")
        void shouldAllowEmptyReplacement() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of(rule));

            level.replaceRules(List.of());

            assertThat(level.getRules()).isEmpty();
        }

        @Test
        @DisplayName("should reject duplicate rule key in replacement set")
        void shouldRejectDuplicateKeyInReplacement() {
            MembershipPaymentRule rule1 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule rule2 = MembershipPaymentRule.fixedAmount(EVENT_TYPE_A, "A",
                    Money.ofCzk(new BigDecimal("200")));
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of());

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
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of());
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            level.addRule(rule);

            assertThat(level.getRules()).hasSize(1);
            assertThat(level.getRules()).contains(rule);
        }

        @Test
        @DisplayName("should allow adding rules for different event type + ranking combinations")
        void shouldAllowDifferentCombinations() {
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of());

            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "B", 30));
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_B, "A", 70));

            assertThat(level.getRules()).hasSize(3);
        }

        @Test
        @DisplayName("should reject duplicate (eventTypeId, rankingShortName) combination")
        void shouldRejectDuplicateRule() {
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of());
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            assertThatThrownBy(() -> level.addRule(
                    MembershipPaymentRule.fixedAmount(EVENT_TYPE_A, "A", Money.ofCzk(new BigDecimal("200")))))
                    .isInstanceOf(DuplicatePaymentRuleException.class);
        }

        @Test
        @DisplayName("should reject null rule")
        void shouldRejectNullRule() {
            MembershipFeeTier level = MembershipFeeTier.create("Závodník", YEARLY_FEE, List.of());

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
            MembershipFeeTierId id = new MembershipFeeTierId(UUID.randomUUID());
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            MembershipFeeTier level = MembershipFeeTier.reconstruct(id, "Dospělý", YEARLY_FEE, List.of(rule), null);

            assertThat(level.getId()).isEqualTo(id);
            assertThat(level.getName()).isEqualTo("Dospělý");
            assertThat(level.getYearlyFee()).isEqualTo(YEARLY_FEE);
            assertThat(level.getRules()).containsExactly(rule);
        }
    }
}
