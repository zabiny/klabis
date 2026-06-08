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

@DisplayName("MembershipFeeGroup domain tests")
class MembershipFeeGroupTest {

    private static final MembershipFeeLevelId SOURCE_LEVEL_ID =
            new MembershipFeeLevelId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final int YEAR = 2026;
    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));
    private static final EventTypeId EVENT_TYPE = EventTypeId.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    private MembershipFeeGroup buildGroup() {
        return MembershipFeeGroup.createSnapshot(
                SOURCE_LEVEL_ID, "Dospělý", YEAR, YEARLY_FEE, List.of());
    }

    @Nested
    @DisplayName("createSnapshot() factory method")
    class CreateSnapshot {

        @Test
        @DisplayName("should create group from snapshot with name, year and yearly fee")
        void shouldCreateGroupWithBasicFields() {
            MembershipFeeGroup group = buildGroup();

            assertThat(group.getId()).isNotNull();
            assertThat(group.getSourceLevelId()).isEqualTo(SOURCE_LEVEL_ID);
            assertThat(group.getName()).isEqualTo("Dospělý");
            assertThat(group.getYear()).isEqualTo(YEAR);
            assertThat(group.getYearlyFeeSnapshot()).isEqualTo(YEARLY_FEE);
        }

        @Test
        @DisplayName("should create group with EDITABLE status")
        void shouldBeEditableInitially() {
            MembershipFeeGroup group = buildGroup();

            assertThat(group.getStatus()).isEqualTo(PublishedLevelStatus.EDITABLE);
        }

        @Test
        @DisplayName("should copy rules snapshot from source level")
        void shouldCopyRulesFromLevel() {
            MembershipPaymentRuleSnapshot rule = new MembershipPaymentRuleSnapshot(
                    EVENT_TYPE, "A", new MembershipPaymentRule.RuleValue.Percentage(50));

            MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                    SOURCE_LEVEL_ID, "Závodník", YEAR, YEARLY_FEE, List.of(rule));

            assertThat(group.getRulesSnapshot()).hasSize(1);
            MembershipPaymentRuleSnapshot retrieved = group.getRulesSnapshot().get(0);
            assertThat(retrieved.eventTypeId()).isEqualTo(EVENT_TYPE);
            assertThat(retrieved.rankingShortName()).isEqualTo("A");
        }

        @Test
        @DisplayName("should start with zero members")
        void shouldStartWithZeroMembers() {
            MembershipFeeGroup group = buildGroup();

            assertThat(group.memberCount()).isZero();
        }
    }

    @Nested
    @DisplayName("editSnapshot()")
    class EditSnapshot {

        @Test
        @DisplayName("should update yearly fee and rules when EDITABLE")
        void shouldUpdateWhenEditable() {
            MembershipFeeGroup group = buildGroup();
            Money newFee = Money.ofCzk(new BigDecimal("1500.00"));
            MembershipPaymentRuleSnapshot rule = new MembershipPaymentRuleSnapshot(
                    EVENT_TYPE, "A", new MembershipPaymentRule.RuleValue.Percentage(60));

            group.editSnapshot(newFee, List.of(rule));

            assertThat(group.getYearlyFeeSnapshot()).isEqualTo(newFee);
            assertThat(group.getRulesSnapshot()).hasSize(1);
        }

        @Test
        @DisplayName("should throw when group is FROZEN")
        void shouldThrowWhenFrozen() {
            MembershipFeeGroup group = buildGroup();
            group.freeze();

            assertThatThrownBy(() -> group.editSnapshot(
                    Money.ofCzk(new BigDecimal("1500.00")), List.of()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("freeze()")
    class Freeze {

        @Test
        @DisplayName("should change status from EDITABLE to FROZEN")
        void shouldChangStatusToFrozen() {
            MembershipFeeGroup group = buildGroup();

            group.freeze();

            assertThat(group.getStatus()).isEqualTo(PublishedLevelStatus.FROZEN);
        }
    }

    @Nested
    @DisplayName("memberCount()")
    class MemberCount {

        @Test
        @DisplayName("should return zero when no members have joined")
        void shouldReturnZeroWithNoMembers() {
            MembershipFeeGroup group = buildGroup();

            assertThat(group.memberCount()).isZero();
        }
    }
}
