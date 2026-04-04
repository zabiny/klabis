package com.klabis.members.familygroup.application;

import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FamilyGroupManagementService")
@ExtendWith(MockitoExtension.class)
class FamilyGroupManagementServiceTest {

    private static final MemberId PARENT_A = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId PARENT_B = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final FamilyGroupId GROUP_ID = new FamilyGroupId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    private FamilyGroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new FamilyGroupManagementService(familyGroupRepository);
    }

    @Nested
    @DisplayName("createFamilyGroup()")
    class CreateFamilyGroupMethod {

        @Test
        @DisplayName("should create group and save it")
        void shouldCreateGroupAndSaveIt() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of());
            when(familyGroupRepository.findByMemberOrParent(any())).thenReturn(Optional.empty());
            when(familyGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyGroup result = service.createFamilyGroup(command);

            assertThat(result.getParents()).containsExactly(PARENT_A);
            assertThat(result.hasMember(PARENT_A)).isTrue();
            verify(familyGroupRepository).save(any(FamilyGroup.class));
        }

        @Test
        @DisplayName("should validate exclusive membership for all parents")
        void shouldRejectParentAlreadyInAnotherFamilyGroup() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of());
            FamilyGroup existingGroup = FamilyGroup.reconstruct(
                    GROUP_ID, "Existující", Set.of(PARENT_A), Set.of(), null);
            when(familyGroupRepository.findByMemberOrParent(PARENT_A)).thenReturn(Optional.of(existingGroup));

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
            when(familyGroupRepository.findByMemberOrParent(PARENT_A)).thenReturn(Optional.empty());
            when(familyGroupRepository.findByMemberOrParent(MEMBER_A)).thenReturn(Optional.of(existingGroup));

            assertThatThrownBy(() -> service.createFamilyGroup(command))
                    .isInstanceOf(MemberAlreadyInFamilyGroupException.class);
        }
    }

    @Nested
    @DisplayName("listFamilyGroups()")
    class ListFamilyGroupsMethod {

        @Test
        @DisplayName("should return all family groups")
        void shouldReturnAllFamilyGroups() {
            FamilyGroup group1 = FamilyGroup.reconstruct(GROUP_ID, "Novákovi", Set.of(PARENT_A), Set.of(), null);
            FamilyGroupId otherId = new FamilyGroupId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
            FamilyGroup group2 = FamilyGroup.reconstruct(otherId, "Svobodovi", Set.of(PARENT_B), Set.of(), null);
            when(familyGroupRepository.findAll()).thenReturn(List.of(group1, group2));

            List<FamilyGroup> result = service.listFamilyGroups();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(FamilyGroup::getName).containsExactlyInAnyOrder("Novákovi", "Svobodovi");
        }

        @Test
        @DisplayName("should return empty list when no groups exist")
        void shouldReturnEmptyListWhenNoGroups() {
            when(familyGroupRepository.findAll()).thenReturn(List.of());

            List<FamilyGroup> result = service.listFamilyGroups();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFamilyGroup()")
    class GetFamilyGroupMethod {

        @Test
        @DisplayName("should return group when found")
        void shouldReturnGroupWhenFound() {
            FamilyGroup group = FamilyGroup.reconstruct(GROUP_ID, "Novákovi", Set.of(PARENT_A), Set.of(), null);
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            FamilyGroup result = service.getFamilyGroup(GROUP_ID);

            assertThat(result.getId()).isEqualTo(GROUP_ID);
            assertThat(result.getName()).isEqualTo("Novákovi");
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFamilyGroup(GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteFamilyGroup()")
    class DeleteFamilyGroupMethod {

        @Test
        @DisplayName("should delete group when it exists")
        void shouldDeleteGroupWhenExists() {
            FamilyGroup group = FamilyGroup.reconstruct(GROUP_ID, "Novákovi", Set.of(PARENT_A), Set.of(), null);
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            service.deleteFamilyGroup(GROUP_ID);

            verify(familyGroupRepository).delete(GROUP_ID);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteFamilyGroup(GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addParent()")
    class AddParentMethod {

        @Test
        @DisplayName("should add parent as owner and member")
        void shouldAddParentAsOwnerAndMember() {
            FamilyGroup group = FamilyGroup.reconstruct(
                    GROUP_ID, "Novákovi", Set.of(PARENT_A), Set.of(), null);
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(familyGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParent(GROUP_ID, PARENT_B);

            ArgumentCaptor<FamilyGroup> captor = ArgumentCaptor.forClass(FamilyGroup.class);
            verify(familyGroupRepository).save(captor.capture());
            FamilyGroup saved = captor.getValue();
            assertThat(saved.getParents()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(saved.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addParent(GROUP_ID, PARENT_B))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeParent()")
    class RemoveParentMethod {

        @Test
        @DisplayName("should remove parent from owners and members")
        void shouldRemoveParentFromOwnersAndMembers() {
            FamilyGroup group = FamilyGroup.reconstruct(
                    GROUP_ID, "Novákovi", Set.of(PARENT_A, PARENT_B),
                    Set.of(GroupMembership.of(PARENT_A.toUserId()), GroupMembership.of(PARENT_B.toUserId())), null);
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(familyGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParent(GROUP_ID, PARENT_B);

            ArgumentCaptor<FamilyGroup> captor = ArgumentCaptor.forClass(FamilyGroup.class);
            verify(familyGroupRepository).save(captor.capture());
            FamilyGroup saved = captor.getValue();
            assertThat(saved.getParents()).containsExactly(PARENT_A);
            assertThat(saved.hasMember(PARENT_B)).isFalse();
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last parent")
        void shouldThrowWhenRemovingLastParent() {
            FamilyGroup group = FamilyGroup.reconstruct(
                    GROUP_ID, "Novákovi", Set.of(PARENT_A),
                    Set.of(GroupMembership.of(PARENT_A.toUserId())), null);
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.removeParent(GROUP_ID, PARENT_A))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(familyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeParent(GROUP_ID, PARENT_A))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }
}
