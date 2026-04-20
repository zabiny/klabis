package com.klabis.groups.freegroup.infrastructure.listeners;

import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspendedEvent;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.RegistrationNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MemberSuspendedListener")
@ExtendWith(MockitoExtension.class)
class MemberSuspendedListenerTest {

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId INVITEE = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final FreeGroupId GROUP_ID = new FreeGroupId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final FreeGroupId SECOND_GROUP_ID = new FreeGroupId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Mock
    private FreeGroupRepository freeGroupRepository;

    private MemberSuspendedListener listener;

    @BeforeEach
    void setUp() {
        listener = new MemberSuspendedListener(freeGroupRepository);
    }

    private MemberSuspendedEvent suspendedEvent(MemberId memberId) {
        return new MemberSuspendedEvent(
                memberId,
                new RegistrationNumber("ZBM9000"),
                DeactivationReason.OTHER,
                Instant.now(),
                OWNER.toUserId(),
                null
        );
    }

    @Nested
    @DisplayName("on(MemberSuspendedEvent) — PENDING invitations")
    class PendingInvitationsScenario {

        @Test
        @DisplayName("should cancel all pending invitations for the deactivated member and save the group")
        void shouldCancelAllPendingInvitationsForDeactivatedMember() {
            Invitation pending = Invitation.createPending(OWNER.toUserId(), INVITEE.toUserId());
            FreeGroup group = FreeGroup.reconstruct(GROUP_ID, "Test Group",
                    Set.of(OWNER),
                    Set.of(GroupMembership.of(OWNER.toUserId())),
                    Set.of(pending),
                    null);

            when(freeGroupRepository.findAll(FreeGroupFilter.all().withPendingInvitationFor(INVITEE)))
                    .thenReturn(List.of(group));
            when(freeGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            listener.on(suspendedEvent(INVITEE));

            assertThat(group.getPendingInvitations()).isEmpty();
            verify(freeGroupRepository).save(group);
        }

        @Test
        @DisplayName("should cancel pending invitations across multiple groups — each group saved once")
        void shouldCancelPendingInvitationsAcrossMultipleGroups() {
            Invitation pending1 = Invitation.createPending(OWNER.toUserId(), INVITEE.toUserId());
            Invitation pending2 = Invitation.createPending(OWNER.toUserId(), INVITEE.toUserId());
            FreeGroup group1 = FreeGroup.reconstruct(GROUP_ID, "Group One",
                    Set.of(OWNER), Set.of(GroupMembership.of(OWNER.toUserId())), Set.of(pending1), null);
            FreeGroup group2 = FreeGroup.reconstruct(SECOND_GROUP_ID, "Group Two",
                    Set.of(OWNER), Set.of(GroupMembership.of(OWNER.toUserId())), Set.of(pending2), null);

            when(freeGroupRepository.findAll(FreeGroupFilter.all().withPendingInvitationFor(INVITEE)))
                    .thenReturn(List.of(group1, group2));
            when(freeGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            listener.on(suspendedEvent(INVITEE));

            verify(freeGroupRepository, times(1)).save(group1);
            verify(freeGroupRepository, times(1)).save(group2);
        }
    }

    @Nested
    @DisplayName("on(MemberSuspendedEvent) — non-PENDING invitations are unchanged")
    class NonPendingInvitationsScenario {

        @Test
        @DisplayName("should not attempt to cancel when the member has no pending invitations")
        void shouldNotCancelWhenNoPendingInvitations() {
            when(freeGroupRepository.findAll(FreeGroupFilter.all().withPendingInvitationFor(INVITEE)))
                    .thenReturn(List.of());

            listener.on(suspendedEvent(INVITEE));

            verify(freeGroupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("on(MemberSuspendedEvent) — partial failure handling")
    class PartialFailureScenario {

        @Test
        @DisplayName("should continue cancelling remaining groups when one save fails")
        void shouldContinueWhenOneGroupSaveFails() {
            Invitation pending1 = Invitation.createPending(OWNER.toUserId(), INVITEE.toUserId());
            Invitation pending2 = Invitation.createPending(OWNER.toUserId(), INVITEE.toUserId());
            FreeGroup group1 = FreeGroup.reconstruct(GROUP_ID, "Group One",
                    Set.of(OWNER), Set.of(GroupMembership.of(OWNER.toUserId())), Set.of(pending1), null);
            FreeGroup group2 = FreeGroup.reconstruct(SECOND_GROUP_ID, "Group Two",
                    Set.of(OWNER), Set.of(GroupMembership.of(OWNER.toUserId())), Set.of(pending2), null);

            when(freeGroupRepository.findAll(FreeGroupFilter.all().withPendingInvitationFor(INVITEE)))
                    .thenReturn(List.of(group1, group2));
            doThrow(new RuntimeException("Simulated failure")).when(freeGroupRepository).save(group1);
            when(freeGroupRepository.save(group2)).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> listener.on(suspendedEvent(INVITEE))).doesNotThrowAnyException();

            verify(freeGroupRepository, times(1)).save(group2);
        }
    }
}
