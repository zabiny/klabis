package com.klabis.groups.membersgroup.application;

import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.groups.membersgroup.domain.GroupOwnershipRequiredException;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.common.usergroup.NotInvitedMemberException;
import com.klabis.members.MemberId;
import com.klabis.groups.common.domain.MembersGroupFilter;
import com.klabis.groups.membersgroup.domain.MembersGroup;
import com.klabis.groups.membersgroup.MembersGroupId;
import com.klabis.groups.membersgroup.domain.MembersGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MembersGroupManagementService")
@ExtendWith(MockitoExtension.class)
class MembersGroupManagementServiceTest {

    private static final MemberId CREATOR = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER_MEMBER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId ANOTHER_MEMBER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final MembersGroupId GROUP_ID = new MembersGroupId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    @Mock
    private MembersGroupRepository membersGroupRepository;

    private MembersGroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new MembersGroupManagementService(membersGroupRepository);
    }

    @Nested
    @DisplayName("createGroup()")
    class CreateGroupMethod {

        @Test
        @DisplayName("should create group and save it")
        void shouldCreateGroupAndSaveIt() {
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MembersGroup result = service.createGroup("Test Group", CREATOR);

            assertThat(result.getOwners()).containsExactly(CREATOR);
            assertThat(result.hasMember(CREATOR)).isTrue();
            assertThat(result.getName()).isEqualTo("Test Group");
            verify(membersGroupRepository).save(any(MembersGroup.class));
        }
    }

    @Nested
    @DisplayName("getGroup()")
    class GetGroupMethod {

        @Test
        @DisplayName("should return group when found")
        void shouldReturnGroupWhenFound() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            MembersGroup result = service.getGroup(GROUP_ID);

            assertThat(result.getId()).isEqualTo(GROUP_ID);
            assertThat(result.getName()).isEqualTo("Test Group");
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getGroup(GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listGroupsForMember()")
    class ListGroupsForMemberMethod {

        @Test
        @DisplayName("should return all groups for member")
        void shouldReturnGroupsForMember() {
            MembersGroupId otherId = new MembersGroupId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
            MembersGroup group1 = MembersGroup.reconstruct(GROUP_ID, "Group A", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            MembersGroup group2 = MembersGroup.reconstruct(otherId, "Group B", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findAll(MembersGroupFilter.all().withOwnerOrMemberIs(CREATOR))).thenReturn(List.of(group1, group2));

            List<MembersGroup> result = service.listGroupsForMember(CREATOR);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(MembersGroup::getName).containsExactlyInAnyOrder("Group A", "Group B");
        }
    }

    @Nested
    @DisplayName("renameGroup()")
    class RenameGroupMethod {

        @Test
        @DisplayName("should rename group and save it")
        void shouldRenameGroupAndSave() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Old Name", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MembersGroup result = service.renameGroup(GROUP_ID, "New Name", CREATOR);

            assertThat(result.getName()).isEqualTo("New Name");
            verify(membersGroupRepository).save(any(MembersGroup.class));
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when acting member is not owner")
        void shouldThrowWhenNotOwner() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Old Name", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.renameGroup(GROUP_ID, "New Name", OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.renameGroup(GROUP_ID, "New Name", CREATOR))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGroup()")
    class DeleteGroupMethod {

        @Test
        @DisplayName("should delete group when owner requests deletion")
        void shouldDeleteGroupWhenExists() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            service.deleteGroup(GROUP_ID, CREATOR);

            verify(membersGroupRepository).delete(GROUP_ID);
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when acting member is not owner")
        void shouldThrowWhenNotOwner() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.deleteGroup(GROUP_ID, OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteGroup(GROUP_ID, CREATOR))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addOwner()")
    class AddOwnerMethod {

        @Test
        @DisplayName("should add owner and save")
        void shouldAddOwnerAndSave() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(OTHER_MEMBER.toUserId())),
                    Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addOwner(GROUP_ID, OTHER_MEMBER, CREATOR);

            ArgumentCaptor<MembersGroup> captor = ArgumentCaptor.forClass(MembersGroup.class);
            verify(membersGroupRepository).save(captor.capture());
            assertThat(captor.getValue().isOwner(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when acting member is not owner")
        void shouldThrowWhenNotOwner() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(OTHER_MEMBER.toUserId())),
                    Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.addOwner(GROUP_ID, OTHER_MEMBER, OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addOwner(GROUP_ID, OTHER_MEMBER, CREATOR))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeOwner()")
    class RemoveOwnerMethod {

        @Test
        @DisplayName("should remove owner and save")
        void shouldRemoveOwnerAndSave() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group",
                    Set.of(CREATOR, OTHER_MEMBER),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(OTHER_MEMBER.toUserId())),
                    Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeOwner(GROUP_ID, OTHER_MEMBER, CREATOR);

            ArgumentCaptor<MembersGroup> captor = ArgumentCaptor.forClass(MembersGroup.class);
            verify(membersGroupRepository).save(captor.capture());
            assertThat(captor.getValue().isOwner(OTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when acting member is not owner")
        void shouldThrowWhenNotOwner() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group",
                    Set.of(CREATOR, OTHER_MEMBER),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(OTHER_MEMBER.toUserId())),
                    Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.removeOwner(GROUP_ID, OTHER_MEMBER, ANOTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last owner")
        void shouldThrowWhenRemovingLastOwner() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.removeOwner(GROUP_ID, CREATOR, CREATOR))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeOwner(GROUP_ID, CREATOR, CREATOR))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberMethod {

        @Test
        @DisplayName("should remove non-owner member and save")
        void shouldRemoveMemberAndSave() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(OTHER_MEMBER.toUserId())),
                    Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeMember(GROUP_ID, OTHER_MEMBER, CREATOR);

            ArgumentCaptor<MembersGroup> captor = ArgumentCaptor.forClass(MembersGroup.class);
            verify(membersGroupRepository).save(captor.capture());
            assertThat(captor.getValue().hasMember(OTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when acting member is not owner")
        void shouldThrowWhenNotOwner() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(OTHER_MEMBER.toUserId())),
                    Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, OTHER_MEMBER, OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, OTHER_MEMBER, CREATOR))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("inviteMember()")
    class InviteMemberMethod {

        @Test
        @DisplayName("should create pending invitation and save")
        void shouldCreatePendingInvitationAndSave() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.inviteMember(GROUP_ID, CREATOR, OTHER_MEMBER);

            ArgumentCaptor<MembersGroup> captor = ArgumentCaptor.forClass(MembersGroup.class);
            verify(membersGroupRepository).save(captor.capture());
            assertThat(captor.getValue().getPendingInvitations()).hasSize(1);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inviteMember(GROUP_ID, CREATOR, OTHER_MEMBER))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("acceptInvitation()")
    class AcceptInvitationMethod {

        @Test
        @DisplayName("should accept invitation and add member when correct member accepts")
        void shouldAcceptInvitationAndAddMember() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.acceptInvitation(GROUP_ID, invitationId, OTHER_MEMBER);

            ArgumentCaptor<MembersGroup> captor = ArgumentCaptor.forClass(MembersGroup.class);
            verify(membersGroupRepository).save(captor.capture());
            assertThat(captor.getValue().hasMember(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should throw NotInvitedMemberException when different member tries to accept")
        void shouldThrowWhenWrongMemberAccepts() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.acceptInvitation(GROUP_ID, invitationId, ANOTHER_MEMBER))
                    .isInstanceOf(NotInvitedMemberException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acceptInvitation(GROUP_ID, InvitationId.newId(), OTHER_MEMBER))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectInvitation()")
    class RejectInvitationMethod {

        @Test
        @DisplayName("should reject invitation when correct member rejects")
        void shouldRejectInvitation() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(membersGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rejectInvitation(GROUP_ID, invitationId, OTHER_MEMBER);

            ArgumentCaptor<MembersGroup> captor = ArgumentCaptor.forClass(MembersGroup.class);
            verify(membersGroupRepository).save(captor.capture());
            Invitation rejected = captor.getValue().getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(rejected.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw NotInvitedMemberException when different member tries to reject")
        void shouldThrowWhenWrongMemberRejects() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.rejectInvitation(GROUP_ID, invitationId, ANOTHER_MEMBER))
                    .isInstanceOf(NotInvitedMemberException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(membersGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectInvitation(GROUP_ID, InvitationId.newId(), OTHER_MEMBER))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getGroupsWithPendingInvitations()")
    class GetGroupsWithPendingInvitationsMethod {

        @Test
        @DisplayName("should return groups with pending invitations for member")
        void shouldReturnGroupsWithPendingInvitations() {
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(), null);
            when(membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(OTHER_MEMBER)))
                    .thenReturn(List.of(group));

            List<MembersGroup> result = service.getGroupsWithPendingInvitations(OTHER_MEMBER);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no pending invitations")
        void shouldReturnEmptyListWhenNoPendingInvitations() {
            when(membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(OTHER_MEMBER)))
                    .thenReturn(List.of());

            List<MembersGroup> result = service.getGroupsWithPendingInvitations(OTHER_MEMBER);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPendingInvitationsForMember()")
    class GetPendingInvitationsForMemberMethod {

        @Test
        @DisplayName("should return flat list of pending invitations for member across groups")
        void shouldReturnFlatListOfPendingInvitations() {
            InvitationId invitationId = InvitationId.newId();
            Invitation invitation = Invitation.reconstruct(
                    invitationId, OTHER_MEMBER.toUserId(), CREATOR.toUserId(), InvitationStatus.PENDING, Instant.now());
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(invitation), null);
            when(membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(OTHER_MEMBER)))
                    .thenReturn(List.of(group));

            List<PendingInvitationView> result = service.getPendingInvitationsForMember(OTHER_MEMBER);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupId()).isEqualTo(GROUP_ID);
            assertThat(result.get(0).groupName()).isEqualTo("Test Group");
            assertThat(result.get(0).invitation().getId()).isEqualTo(invitationId);
        }

        @Test
        @DisplayName("should filter out invitations not addressed to the requesting member")
        void shouldFilterInvitationsForOtherMembers() {
            InvitationId invForOther = InvitationId.newId();
            Invitation otherInvitation = Invitation.reconstruct(
                    invForOther, ANOTHER_MEMBER.toUserId(), CREATOR.toUserId(), InvitationStatus.PENDING, Instant.now());
            MembersGroup group = MembersGroup.reconstruct(GROUP_ID, "Test Group", Set.of(CREATOR),
                    Set.of(GroupMembership.of(CREATOR.toUserId())), Set.of(otherInvitation), null);
            when(membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(OTHER_MEMBER)))
                    .thenReturn(List.of(group));

            List<PendingInvitationView> result = service.getPendingInvitationsForMember(OTHER_MEMBER);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no groups have pending invitations")
        void shouldReturnEmptyListWhenNoGroups() {
            when(membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(OTHER_MEMBER)))
                    .thenReturn(List.of());

            List<PendingInvitationView> result = service.getPendingInvitationsForMember(OTHER_MEMBER);

            assertThat(result).isEmpty();
        }
    }
}
