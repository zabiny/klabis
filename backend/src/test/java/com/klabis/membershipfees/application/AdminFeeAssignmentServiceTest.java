package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminFeeAssignmentService")
class AdminFeeAssignmentServiceTest {

    private static final MemberId ADMIN_ID = new MemberId(UUID.fromString("a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"));
    private static final MemberId TARGET_MEMBER_ID = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final MembershipFeeTierId LEVEL_ID_A =
            new MembershipFeeTierId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MembershipFeeTierId LEVEL_ID_B =
            new MembershipFeeTierId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final int YEAR = 2026;

    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private FeeYearPublicationRepository publicationRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminFeeAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AdminFeeAssignmentService(groupRepository, publicationRepository, eventPublisher);
    }

    private static final LocalDate VOTING_DEADLINE = LocalDate.of(YEAR, 3, 31);

    private MembershipFeeGroup buildFrozenGroup(MembershipFeeTierId sourceLevelId) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(UUID.randomUUID()),
                sourceLevelId,
                "Frozen Group", YEAR, VOTING_DEADLINE,
                Money.ofCzk(new BigDecimal("1200.00")),
                PublishedLevelStatus.FROZEN,
                List.of(), Set.of(), null);
    }

    private MembershipFeeGroup buildEditableGroup(MembershipFeeTierId sourceLevelId) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(UUID.randomUUID()),
                sourceLevelId,
                "Editable Group", YEAR, VOTING_DEADLINE,
                Money.ofCzk(new BigDecimal("1200.00")),
                PublishedLevelStatus.EDITABLE,
                List.of(), Set.of(), null);
    }

    private FeeYearPublication buildClosedPublication() {
        return FeeYearPublication.reconstruct(
                new com.klabis.membershipfees.FeeYearPublicationId(UUID.randomUUID()),
                YEAR,
                LocalDate.now().minusDays(1),
                null,
                List.of());
    }

    @Nested
    @DisplayName("assignLevel() — admin assigns after deadline")
    class AssignLevel {

        @Test
        @DisplayName("should assign member to FROZEN group bypassing deadline check")
        void shouldAssignMemberToFrozenGroupWithoutDeadlineCheck() {
            MembershipFeeGroup frozenGroup = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = frozenGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(frozenGroup));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(TARGET_MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, groupId, YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(TARGET_MEMBER_ID)));
        }

        @Test
        @DisplayName("should record ADMIN_ASSIGNMENT as source on the membership")
        void shouldRecordAdminAssignmentSource() {
            MembershipFeeGroup frozenGroup = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = frozenGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(frozenGroup));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(TARGET_MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, groupId, YEAR));

            ArgumentCaptor<MembershipFeeGroup> captor = ArgumentCaptor.forClass(MembershipFeeGroup.class);
            verify(groupRepository).save(captor.capture());

            FeeGroupMembership membership = captor.getValue().getMemberships().iterator().next();
            assertThat(membership.source()).isEqualTo(AssignmentSource.ADMIN_ASSIGNMENT);
        }

        @Test
        @DisplayName("should record the adminId as assignedBy on the membership")
        void shouldRecordAdminIdAsAssignedBy() {
            MembershipFeeGroup frozenGroup = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = frozenGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(frozenGroup));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(TARGET_MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, groupId, YEAR));

            ArgumentCaptor<MembershipFeeGroup> captor = ArgumentCaptor.forClass(MembershipFeeGroup.class);
            verify(groupRepository).save(captor.capture());

            FeeGroupMembership membership = captor.getValue().getMemberships().iterator().next();
            assertThat(membership.assignedBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("should move member from old group to new group when changing assignment after deadline")
        void shouldMoveMemberBetweenGroupsAfterDeadline() {
            Set<FeeGroupMembership> existingMemberships = Set.of(
                    new FeeGroupMembership(TARGET_MEMBER_ID, LocalDate.of(YEAR, 1, 5), AssignmentSource.MEMBER_CHOICE, null));
            MembershipFeeGroup oldGroup = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    LEVEL_ID_A, "Old Frozen Group", YEAR, VOTING_DEADLINE,
                    Money.ofCzk(new BigDecimal("1200.00")),
                    PublishedLevelStatus.FROZEN,
                    List.of(), existingMemberships, null);
            MembershipFeeGroup newGroup = buildFrozenGroup(LEVEL_ID_B);

            MembershipFeeGroupId newGroupId = newGroup.getId();
            when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(newGroup));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(TARGET_MEMBER_ID, YEAR)).thenReturn(Optional.of(oldGroup));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, newGroupId, YEAR));

            verify(groupRepository).save(argThat(g -> !g.hasMember(TARGET_MEMBER_ID) && g.getId().equals(oldGroup.getId())));
            verify(groupRepository).save(argThat(g -> g.hasMember(TARGET_MEMBER_ID) && g.getId().equals(newGroupId)));
        }

        @Test
        @DisplayName("should assign member to EDITABLE group as well (admin works regardless of status)")
        void shouldAssignMemberToEditableGroupToo() {
            MembershipFeeGroup editableGroup = buildEditableGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = editableGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(editableGroup));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(TARGET_MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, groupId, YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(TARGET_MEMBER_ID)));
        }

        @Test
        @DisplayName("should throw MembershipFeeGroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            MembershipFeeGroupId unknownGroupId = new MembershipFeeGroupId(UUID.randomUUID());
            when(groupRepository.findById(unknownGroupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, unknownGroupId, YEAR)))
                    .isInstanceOf(MembershipFeeGroupNotFoundException.class);
        }

        @Test
        @DisplayName("should throw FeeYearPublicationNotFoundException when no publication for year")
        void shouldThrowWhenNoPublication() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, groupId, YEAR)))
                    .isInstanceOf(FeeYearPublicationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignLevel() — sanction lifting")
    class SanctionLifting {

        @Test
        @DisplayName("should always publish MemberFeeSelectionResolvedEvent on admin assignment")
        void shouldPublishResolvedEventOnAdminAssignment() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(TARGET_MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                    ADMIN_ID, TARGET_MEMBER_ID, groupId, YEAR));

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(MemberFeeSelectionResolvedEvent.class);
            MemberFeeSelectionResolvedEvent event = (MemberFeeSelectionResolvedEvent) eventCaptor.getValue();
            assertThat(event.memberId()).isEqualTo(TARGET_MEMBER_ID);
            assertThat(event.year()).isEqualTo(YEAR);
        }
    }
}
