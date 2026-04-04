package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.GroupMembership;
import com.klabis.usergroups.domain.GroupType;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GroupManagementService — FamilyGroup operations")
@ExtendWith(MockitoExtension.class)
class GroupManagementServiceFamilyGroupTest {

    private static final MemberId PARENT_A = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId PARENT_B = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final UserGroupId GROUP_ID = new UserGroupId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    @Mock
    private UserGroupRepository userGroupRepository;

    private GroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new GroupManagementService(userGroupRepository, null);
    }

    @Nested
    @DisplayName("createFamilyGroup()")
    class CreateFamilyGroupMethod {

        @Test
        @DisplayName("should create group and add all parents as owners and members")
        void shouldCreateGroupWithParentsAsOwnersAndMembers() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of());
            when(userGroupRepository.findOne(any())).thenReturn(Optional.empty());
            when(userGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyGroup result = service.createFamilyGroup(command);

            assertThat(result.getParents()).containsExactly(PARENT_A);
            assertThat(result.hasMember(PARENT_A)).isTrue();
        }

        @Test
        @DisplayName("should validate exclusive membership for all parents")
        void shouldRejectParentAlreadyInAnotherFamilyGroup() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of());
            FamilyGroup existingGroup = FamilyGroup.reconstruct(
                    GROUP_ID, "Existující", Set.of(PARENT_A), Set.of(), null);
            when(userGroupRepository.findOne(GroupFilter.byTypeAndMemberOrOwner(GroupType.FAMILY, PARENT_A)))
                    .thenReturn(Optional.of(existingGroup));

            assertThatThrownBy(() -> service.createFamilyGroup(command))
                    .isInstanceOf(MemberAlreadyInFamilyGroupException.class);
        }

        @Test
        @DisplayName("should validate exclusive membership for initial members")
        void shouldRejectInitialMemberAlreadyInAnotherFamilyGroup() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of(MEMBER_A));
            FamilyGroup existingGroup = FamilyGroup.reconstruct(
                    GROUP_ID, "Existující", Set.of(PARENT_B), Set.of(), null);
            when(userGroupRepository.findOne(GroupFilter.byTypeAndMemberOrOwner(GroupType.FAMILY, PARENT_A)))
                    .thenReturn(Optional.empty());
            when(userGroupRepository.findOne(GroupFilter.byTypeAndMemberOrOwner(GroupType.FAMILY, MEMBER_A)))
                    .thenReturn(Optional.of(existingGroup));

            assertThatThrownBy(() -> service.createFamilyGroup(command))
                    .isInstanceOf(MemberAlreadyInFamilyGroupException.class);
        }
    }

    @Nested
    @DisplayName("addParentToFamilyGroup()")
    class AddParentToFamilyGroupMethod {

        @Test
        @DisplayName("should add parent as owner and member")
        void shouldAddParentAsOwnerAndMember() {
            FamilyGroup group = FamilyGroup.reconstruct(
                    GROUP_ID, "Novákovi", Set.of(PARENT_A), Set.of(), null);
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(userGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParentToFamilyGroup(GROUP_ID, PARENT_B);

            ArgumentCaptor<FamilyGroup> captor = ArgumentCaptor.forClass(FamilyGroup.class);
            verify(userGroupRepository).save(captor.capture());
            FamilyGroup saved = captor.getValue();
            assertThat(saved.getParents()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(saved.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addParentToFamilyGroup(GROUP_ID, PARENT_B))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group is not a family group")
        void shouldThrowWhenGroupIsNotFamilyGroup() {
            FreeGroup freeGroup = FreeGroup.create(new FreeGroup.CreateFreeGroup("Trail Runners", PARENT_A));
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(freeGroup));

            assertThatThrownBy(() -> service.addParentToFamilyGroup(GROUP_ID, PARENT_B))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeParentFromFamilyGroup()")
    class RemoveParentFromFamilyGroupMethod {

        @Test
        @DisplayName("should remove parent from owners and members")
        void shouldRemoveParentFromOwnersAndMembers() {
            FamilyGroup group = FamilyGroup.reconstruct(
                    GROUP_ID, "Novákovi", Set.of(PARENT_A, PARENT_B),
                    Set.of(GroupMembership.of(PARENT_A), GroupMembership.of(PARENT_B)), null);
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(userGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParentFromFamilyGroup(GROUP_ID, PARENT_B);

            ArgumentCaptor<FamilyGroup> captor = ArgumentCaptor.forClass(FamilyGroup.class);
            verify(userGroupRepository).save(captor.capture());
            FamilyGroup saved = captor.getValue();
            assertThat(saved.getParents()).containsExactly(PARENT_A);
            assertThat(saved.hasMember(PARENT_B)).isFalse();
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last parent")
        void shouldThrowWhenRemovingLastParent() {
            FamilyGroup group = FamilyGroup.reconstruct(
                    GROUP_ID, "Novákovi", Set.of(PARENT_A),
                    Set.of(GroupMembership.of(PARENT_A)), null);
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.removeParentFromFamilyGroup(GROUP_ID, PARENT_A))
                    .isInstanceOf(UserGroup.CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeParentFromFamilyGroup(GROUP_ID, PARENT_A))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }
}
