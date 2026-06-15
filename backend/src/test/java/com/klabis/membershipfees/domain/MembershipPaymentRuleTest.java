package com.klabis.membershipfees.domain;

import com.klabis.finance.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MembershipPaymentRule value object tests")
class MembershipPaymentRuleTest {

    private static final EventTypeReference EVENT_TYPE_A = EventTypeReference.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final EventTypeReference EVENT_TYPE_B = EventTypeReference.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Nested
    @DisplayName("Percentage rule")
    class PercentageRule {

        @Test
        @DisplayName("should create percentage rule with 0 percent")
        void shouldCreateZeroPercent() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 0);

            assertThat(rule.eventTypeId()).isEqualTo(EVENT_TYPE_A);
            assertThat(rule.rankingShortName()).isEqualTo("A");
            assertThat(rule.value()).isInstanceOf(MembershipPaymentRule.RuleValue.Percentage.class);
            assertThat(((MembershipPaymentRule.RuleValue.Percentage) rule.value()).percent()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create percentage rule with 100 percent")
        void shouldCreateHundredPercent() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 100);

            assertThat(((MembershipPaymentRule.RuleValue.Percentage) rule.value()).percent()).isEqualTo(100);
        }

        @Test
        @DisplayName("should create percentage rule above 100 percent (surcharge above base price)")
        void shouldAllowAboveHundredPercent() {
            MembershipPaymentRule rule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 150);

            assertThat(((MembershipPaymentRule.RuleValue.Percentage) rule.value()).percent()).isEqualTo(150);
        }

        @Test
        @DisplayName("should reject negative percentage")
        void shouldRejectNegativePercentage() {
            assertThatThrownBy(() -> MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null eventTypeId")
        void shouldRejectNullEventTypeReference() {
            assertThatThrownBy(() -> MembershipPaymentRule.percentage(null, "A", 50))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject blank rankingShortName")
        void shouldRejectBlankRankingShortName() {
            assertThatThrownBy(() -> MembershipPaymentRule.percentage(EVENT_TYPE_A, "", 50))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null rankingShortName")
        void shouldRejectNullRankingShortName() {
            assertThatThrownBy(() -> MembershipPaymentRule.percentage(EVENT_TYPE_A, null, 50))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Fixed amount rule")
    class FixedAmountRule {

        @Test
        @DisplayName("should create fixed amount rule with CZK amount")
        void shouldCreateFixedAmountRule() {
            Money amount = Money.ofCzk(new BigDecimal("200.00"));
            MembershipPaymentRule rule = MembershipPaymentRule.fixedAmount(EVENT_TYPE_B, "LOB", amount);

            assertThat(rule.eventTypeId()).isEqualTo(EVENT_TYPE_B);
            assertThat(rule.rankingShortName()).isEqualTo("LOB");
            assertThat(rule.value()).isInstanceOf(MembershipPaymentRule.RuleValue.FixedAmount.class);
            assertThat(((MembershipPaymentRule.RuleValue.FixedAmount) rule.value()).amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> MembershipPaymentRule.fixedAmount(EVENT_TYPE_B, "LOB", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null eventTypeId")
        void shouldRejectNullEventTypeReference() {
            Money amount = Money.ofCzk(new BigDecimal("200.00"));
            assertThatThrownBy(() -> MembershipPaymentRule.fixedAmount(null, "LOB", amount))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Rule key equality — (eventTypeId, rankingShortName)")
    class RuleKeyEquality {

        @Test
        @DisplayName("two rules with same eventTypeId and ranking are key-equal regardless of value type")
        void sameKeyDifferentValueTypes() {
            MembershipPaymentRule percentageRule = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule fixedRule = MembershipPaymentRule.fixedAmount(EVENT_TYPE_A, "A",
                    Money.ofCzk(new BigDecimal("200")));

            assertThat(percentageRule.hasSameKey(fixedRule)).isTrue();
        }

        @Test
        @DisplayName("rules with different eventTypeId are not key-equal")
        void differentEventType() {
            MembershipPaymentRule rule1 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule rule2 = MembershipPaymentRule.percentage(EVENT_TYPE_B, "A", 50);

            assertThat(rule1.hasSameKey(rule2)).isFalse();
        }

        @Test
        @DisplayName("rules with different rankingShortName are not key-equal")
        void differentRanking() {
            MembershipPaymentRule rule1 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "A", 50);
            MembershipPaymentRule rule2 = MembershipPaymentRule.percentage(EVENT_TYPE_A, "B", 50);

            assertThat(rule1.hasSameKey(rule2)).isFalse();
        }
    }

    @Nested
    @DisplayName("RuleValue sealed type")
    class RuleValueTests {

        @Test
        @DisplayName("Percentage is a RuleValue")
        void percentageIsRuleValue() {
            MembershipPaymentRule.RuleValue value = new MembershipPaymentRule.RuleValue.Percentage(50);

            assertThat(value).isInstanceOf(MembershipPaymentRule.RuleValue.class);
        }

        @Test
        @DisplayName("FixedAmount is a RuleValue")
        void fixedAmountIsRuleValue() {
            MembershipPaymentRule.RuleValue value = new MembershipPaymentRule.RuleValue.FixedAmount(
                    Money.ofCzk(new BigDecimal("200")));

            assertThat(value).isInstanceOf(MembershipPaymentRule.RuleValue.class);
        }
    }
}
