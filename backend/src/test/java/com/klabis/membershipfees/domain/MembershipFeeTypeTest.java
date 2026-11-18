package com.klabis.membershipfees.domain;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTypeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MembershipFeeType domain unit tests")
class MembershipFeeTypeTest {

    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));
    private static final Money OTHER_FEE = Money.ofCzk(new BigDecimal("800.00"));

    private static final EventTypeReference EVENT_TYPE_A = EventTypeReference.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final EventTypeReference EVENT_TYPE_B = EventTypeReference.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Nested
    @DisplayName("MembershipFeeTypeId")
    class IdTests {

        @Test
        @DisplayName("should wrap UUID value")
        void shouldWrapUuidValue() {
            UUID uuid = UUID.randomUUID();
            MembershipFeeTypeId id = new MembershipFeeTypeId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("two IDs with the same UUID should be equal")
        void shouldBeEqualWhenSameUuid() {
            UUID uuid = UUID.randomUUID();
            assertThat(new MembershipFeeTypeId(uuid)).isEqualTo(new MembershipFeeTypeId(uuid));
        }

        @Test
        @DisplayName("two IDs with different UUIDs should not be equal")
        void shouldNotBeEqualWhenDifferentUuid() {
            assertThat(new MembershipFeeTypeId(UUID.randomUUID()))
                    .isNotEqualTo(new MembershipFeeTypeId(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("MembershipFeeType.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create type with name and yearly fee only — no rules argument")
        void shouldCreateTypeWithNameAndYearlyFee() {
            MembershipFeeType type = MembershipFeeType.create("Dospělý", YEARLY_FEE);

            assertThat(type.getId()).isNotNull();
            assertThat(type.getName()).isEqualTo("Dospělý");
            assertThat(type.getYearlyFee()).isEqualTo(YEARLY_FEE);
        }

        @Test
        @DisplayName("should start with empty rules list")
        void shouldStartWithEmptyRules() {
            MembershipFeeType type = MembershipFeeType.create("Dospělý", YEARLY_FEE);

            assertThat(type.getRules()).isEmpty();
        }

        @Test
        @DisplayName("should generate unique IDs for each created type")
        void shouldGenerateUniqueIds() {
            MembershipFeeType type1 = MembershipFeeType.create("Level A", YEARLY_FEE);
            MembershipFeeType type2 = MembershipFeeType.create("Level B", YEARLY_FEE);

            assertThat(type1.getId()).isNotEqualTo(type2.getId());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> MembershipFeeType.create("", YEARLY_FEE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> MembershipFeeType.create(null, YEARLY_FEE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null yearly fee")
        void shouldRejectNullYearlyFee() {
            assertThatThrownBy(() -> MembershipFeeType.create("Dospělý", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("editName()")
    class EditName {

        @Test
        @DisplayName("should update the name")
        void shouldUpdateName() {
            MembershipFeeType level = MembershipFeeType.create("Old Name", YEARLY_FEE);

            level.editName("New Name");

            assertThat(level.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            MembershipFeeType level = MembershipFeeType.create("Some Name", YEARLY_FEE);

            assertThatThrownBy(() -> level.editName(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            MembershipFeeType level = MembershipFeeType.create("Some Name", YEARLY_FEE);

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
            MembershipFeeType level = MembershipFeeType.create("Dospělý", YEARLY_FEE);

            level.editYearlyFee(OTHER_FEE);

            assertThat(level.getYearlyFee()).isEqualTo(OTHER_FEE);
        }

        @Test
        @DisplayName("should reject null fee")
        void shouldRejectNullFee() {
            MembershipFeeType level = MembershipFeeType.create("Dospělý", YEARLY_FEE);

            assertThatThrownBy(() -> level.editYearlyFee(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addRule()")
    class AddRule {

        @Test
        @DisplayName("should add a new rule to the level")
        void shouldAddRule() {
            MembershipFeeType level = MembershipFeeType.create("Závodník", YEARLY_FEE);
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            level.addRule(rule);

            assertThat(level.getRules()).hasSize(1);
            assertThat(level.getRules()).contains(rule);
        }

        @Test
        @DisplayName("should allow adding rules for different event type + ranking combinations")
        void shouldAllowDifferentCombinations() {
            MembershipFeeType level = MembershipFeeType.create("Závodník", YEARLY_FEE);

            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "B", 30));
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_B, "A", 70));

            assertThat(level.getRules()).hasSize(3);
        }

        @Test
        @DisplayName("should reject duplicate (eventTypeId, rankingShortName) combination")
        void shouldRejectDuplicateRule() {
            MembershipFeeType level = MembershipFeeType.create("Závodník", YEARLY_FEE);
            level.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            assertThatThrownBy(() -> level.addRule(
                    MembershipPaymentRule.fixedAmount(EVENT_TYPE_A, "A", Money.ofCzk(new BigDecimal("200")))))
                    .isInstanceOf(DuplicatePaymentRuleException.class);
        }

        @Test
        @DisplayName("should reject null rule")
        void shouldRejectNullRule() {
            MembershipFeeType level = MembershipFeeType.create("Závodník", YEARLY_FEE);

            assertThatThrownBy(() -> level.addRule(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("editRuleValue()")
    class EditRuleValue {

        @Test
        @DisplayName("should update the value of an existing rule, keeping the key unchanged")
        void shouldUpdateRuleValue() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            type.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            type.editRuleValue(EVENT_TYPE_A, "A", new MembershipPaymentRule.RuleValue.Percentage(75));

            MembershipPaymentRule updated = type.getRules().get(0);
            assertThat(updated.eventTypeId()).isEqualTo(EVENT_TYPE_A);
            assertThat(updated.rankingShortName()).isEqualTo("A");
            assertThat(updated.value()).isEqualTo(new MembershipPaymentRule.RuleValue.Percentage(75));
        }

        @Test
        @DisplayName("should update percentage rule to fixed amount rule")
        void shouldUpdatePercentageToFixedAmount() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            type.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "B", 30));
            Money newAmount = Money.ofCzk(new BigDecimal("200.00"));

            type.editRuleValue(EVENT_TYPE_A, "B", new MembershipPaymentRule.RuleValue.FixedAmount(newAmount));

            MembershipPaymentRule updated = type.getRules().get(0);
            assertThat(updated.eventTypeId()).isEqualTo(EVENT_TYPE_A);
            assertThat(updated.rankingShortName()).isEqualTo("B");
            assertThat(updated.value()).isEqualTo(new MembershipPaymentRule.RuleValue.FixedAmount(newAmount));
        }

        @Test
        @DisplayName("should not affect other rules when editing one rule's value")
        void shouldNotAffectOtherRules() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            MembershipPaymentRule ruleA = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule ruleB = MembershipPaymentRule.percentage(EVENT_TYPE_B, "A", 30);
            type.addRule(ruleA);
            type.addRule(ruleB);

            type.editRuleValue(EVENT_TYPE_A, "A", new MembershipPaymentRule.RuleValue.Percentage(90));

            assertThat(type.getRules()).hasSize(2);
            assertThat(type.getRules().stream()
                    .filter(r -> r.eventTypeId().equals(EVENT_TYPE_B) && r.rankingShortName().equals("A"))
                    .findFirst()).isPresent().get()
                    .extracting(MembershipPaymentRule::value)
                    .isEqualTo(new MembershipPaymentRule.RuleValue.Percentage(30));
        }

        @Test
        @DisplayName("should throw PaymentRuleNotFoundException when no rule with the given key exists")
        void shouldThrowWhenRuleNotFound() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);

            assertThatThrownBy(() ->
                    type.editRuleValue(EVENT_TYPE_A, "A", new MembershipPaymentRule.RuleValue.Percentage(50)))
                    .isInstanceOf(PaymentRuleNotFoundException.class);
        }

        @Test
        @DisplayName("should throw PaymentRuleNotFoundException when rule exists for the event type but different ranking")
        void shouldThrowWhenRankingDoesNotMatch() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            type.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            assertThatThrownBy(() ->
                    type.editRuleValue(EVENT_TYPE_A, "B", new MembershipPaymentRule.RuleValue.Percentage(50)))
                    .isInstanceOf(PaymentRuleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeRule()")
    class RemoveRule {

        @Test
        @DisplayName("should remove the matching rule from the list")
        void shouldRemoveMatchingRule() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            type.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            type.removeRule(EVENT_TYPE_A, "A");

            assertThat(type.getRules()).isEmpty();
        }

        @Test
        @DisplayName("should leave other rules untouched when removing one rule")
        void shouldLeaveOtherRulesUntouched() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            MembershipPaymentRule ruleA = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule ruleB = MembershipPaymentRule.percentage(EVENT_TYPE_B, "A", 30);
            type.addRule(ruleA);
            type.addRule(ruleB);

            type.removeRule(EVENT_TYPE_A, "A");

            assertThat(type.getRules()).containsExactly(ruleB);
        }

        @Test
        @DisplayName("should throw PaymentRuleNotFoundException when no rule matches the key")
        void shouldThrowWhenRuleNotFound() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);

            assertThatThrownBy(() -> type.removeRule(EVENT_TYPE_A, "A"))
                    .isInstanceOf(PaymentRuleNotFoundException.class);
        }

        @Test
        @DisplayName("should throw PaymentRuleNotFoundException when ranking does not match")
        void shouldThrowWhenRankingDoesNotMatch() {
            MembershipFeeType type = MembershipFeeType.create("Závodník", YEARLY_FEE);
            type.addRule(MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50));

            assertThatThrownBy(() -> type.removeRule(EVENT_TYPE_A, "B"))
                    .isInstanceOf(PaymentRuleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class Reconstruct {

        @Test
        @DisplayName("should reconstruct level with all fields")
        void shouldReconstructWithAllFields() {
            MembershipFeeTypeId id = new MembershipFeeTypeId(UUID.randomUUID());
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);

            MembershipFeeType level = MembershipFeeType.reconstruct(id, "Dospělý", YEARLY_FEE, List.of(rule), null);

            assertThat(level.getId()).isEqualTo(id);
            assertThat(level.getName()).isEqualTo("Dospělý");
            assertThat(level.getYearlyFee()).isEqualTo(YEARLY_FEE);
            assertThat(level.getRules()).containsExactly(rule);
        }
    }
}
