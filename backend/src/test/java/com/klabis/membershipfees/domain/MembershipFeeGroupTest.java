package com.klabis.membershipfees.domain;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MembershipFeeGroup domain tests")
class MembershipFeeGroupTest {

    private static final MembershipFeeLevelId SOURCE_LEVEL_ID =
            new MembershipFeeLevelId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final int YEAR = 2026;
    private static final Money YEARLY_FEE = Money.ofCzk(new BigDecimal("1200.00"));
    private static final EventTypeReference EVENT_TYPE = EventTypeReference.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final LocalDate VOTING_DEADLINE = LocalDate.of(YEAR, 3, 31);

    private MembershipFeeGroup buildGroup() {
        return MembershipFeeGroup.createSnapshot(
                SOURCE_LEVEL_ID, "Dospělý", YEAR, YEARLY_FEE, List.of(), VOTING_DEADLINE);
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
            MembershipPaymentRule rule = new MembershipPaymentRule(
                    EVENT_TYPE, "A", new MembershipPaymentRule.RuleValue.Percentage(50));

            MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                    SOURCE_LEVEL_ID, "Závodník", YEAR, YEARLY_FEE, List.of(rule), VOTING_DEADLINE);

            assertThat(group.getRulesSnapshot()).hasSize(1);
            MembershipPaymentRule retrieved = group.getRulesSnapshot().get(0);
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
            MembershipPaymentRule rule = new MembershipPaymentRule(
                    EVENT_TYPE, "A", new MembershipPaymentRule.RuleValue.Percentage(60));

            group.editSnapshot(newFee, List.of(rule));

            assertThat(group.getYearlyFeeSnapshot()).isEqualTo(newFee);
            assertThat(group.getRulesSnapshot()).hasSize(1);
        }

        @Test
        @DisplayName("should throw SnapshotFrozenException when group is FROZEN")
        void shouldThrowWhenFrozen() {
            MembershipFeeGroup group = buildGroup();
            group.freeze();

            assertThatThrownBy(() -> group.editSnapshot(
                    Money.ofCzk(new BigDecimal("1500.00")), List.of()))
                    .isInstanceOf(SnapshotFrozenException.class);
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

    @Nested
    @DisplayName("addMember()")
    class AddMember {

        private static final MemberId MEMBER_A = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        @Test
        @DisplayName("should add member with MEMBER_CHOICE to an EDITABLE group")
        void shouldAddMemberWhenEditable() {
            MembershipFeeGroup group = buildGroup();
            LocalDate today = LocalDate.of(YEAR, 1, 10);

            group.addMember(MEMBER_A, today, AssignmentSource.MEMBER_CHOICE);

            assertThat(group.memberCount()).isEqualTo(1);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }

        @Test
        @DisplayName("should record the AssignmentSource on the membership")
        void shouldRecordAssignmentSource() {
            MembershipFeeGroup group = buildGroup();
            LocalDate today = LocalDate.of(YEAR, 1, 10);

            group.addMember(MEMBER_A, today, AssignmentSource.MEMBER_CHOICE);

            FeeGroupMembership membership = group.getMemberships().iterator().next();
            assertThat(membership.source()).isEqualTo(AssignmentSource.MEMBER_CHOICE);
            assertThat(membership.memberId()).isEqualTo(MEMBER_A);
        }

        @Test
        @DisplayName("should throw VotingClosedException when MEMBER_CHOICE on a FROZEN group")
        void shouldThrowWhenFrozenAndMemberChoice() {
            MembershipFeeGroup group = buildGroup();
            group.freeze();
            LocalDate today = LocalDate.of(YEAR, 4, 1);

            assertThatThrownBy(() -> group.addMember(MEMBER_A, today, AssignmentSource.MEMBER_CHOICE))
                    .isInstanceOf(VotingClosedException.class);
        }

        @Test
        @DisplayName("should allow ADMIN_ASSIGNMENT even when group is FROZEN")
        void shouldAllowAdminAssignmentWhenFrozen() {
            MembershipFeeGroup group = buildGroup();
            group.freeze();
            LocalDate today = LocalDate.of(YEAR, 4, 1);

            group.addMember(MEMBER_A, today, AssignmentSource.ADMIN_ASSIGNMENT);

            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }

        @Test
        @DisplayName("should not add duplicate member")
        void shouldNotAddDuplicateMember() {
            MembershipFeeGroup group = buildGroup();
            LocalDate today = LocalDate.of(YEAR, 1, 10);

            group.addMember(MEMBER_A, today, AssignmentSource.MEMBER_CHOICE);
            group.addMember(MEMBER_A, today, AssignmentSource.MEMBER_CHOICE);

            assertThat(group.memberCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw VotingClosedException for MEMBER_CHOICE after votingDeadline (date-based guard)")
        void shouldThrowWhenTodayIsAfterDeadline() {
            MembershipFeeGroup group = buildGroup();
            LocalDate afterDeadline = VOTING_DEADLINE.plusDays(1);

            assertThatThrownBy(() -> group.addMember(MEMBER_A, afterDeadline, AssignmentSource.MEMBER_CHOICE))
                    .isInstanceOf(VotingClosedException.class);
        }

        @Test
        @DisplayName("should allow MEMBER_CHOICE on the deadline day itself")
        void shouldAllowChoiceOnDeadlineDay() {
            MembershipFeeGroup group = buildGroup();

            group.addMember(MEMBER_A, VOTING_DEADLINE, AssignmentSource.MEMBER_CHOICE);

            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }

        @Test
        @DisplayName("should allow ADMIN_ASSIGNMENT after votingDeadline regardless of date")
        void shouldAllowAdminAssignmentAfterDeadline() {
            MembershipFeeGroup group = buildGroup();
            LocalDate afterDeadline = VOTING_DEADLINE.plusDays(10);

            group.addMember(MEMBER_A, afterDeadline, AssignmentSource.ADMIN_ASSIGNMENT);

            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMember {

        private static final MemberId MEMBER_A = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        @Test
        @DisplayName("should remove an existing member")
        void shouldRemoveMember() {
            MembershipFeeGroup group = buildGroup();
            LocalDate today = LocalDate.of(YEAR, 1, 10);
            group.addMember(MEMBER_A, today, AssignmentSource.MEMBER_CHOICE);

            group.removeMember(MEMBER_A);

            assertThat(group.hasMember(MEMBER_A)).isFalse();
            assertThat(group.memberCount()).isZero();
        }

        @Test
        @DisplayName("should be a no-op when member is not present")
        void shouldBeNoOpWhenMemberAbsent() {
            MembershipFeeGroup group = buildGroup();

            assertThatNoException().isThrownBy(() -> group.removeMember(MEMBER_A));
            assertThat(group.memberCount()).isZero();
        }
    }

    @Nested
    @DisplayName("hasMember()")
    class HasMember {

        private static final MemberId MEMBER_A = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        @Test
        @DisplayName("should return false for a group with no members")
        void shouldReturnFalseWhenEmpty() {
            assertThat(buildGroup().hasMember(MEMBER_A)).isFalse();
        }

        @Test
        @DisplayName("should return true after member is added")
        void shouldReturnTrueAfterAdding() {
            MembershipFeeGroup group = buildGroup();
            group.addMember(MEMBER_A, LocalDate.of(YEAR, 1, 10), AssignmentSource.MEMBER_CHOICE);

            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }
    }
}
