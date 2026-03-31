package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@DisplayName("GroupManagementService — training group operations")
@ExtendWith(MockitoExtension.class)
class TrainingGroupManagementServiceTest {

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserGroupId GROUP_ID = new UserGroupId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    @Mock
    private UserGroupRepository userGroupRepository;

    private GroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new GroupManagementService(userGroupRepository);
    }

    @Nested
    @DisplayName("createTrainingGroup()")
    class CreateTrainingGroupMethod {

        @Test
        @DisplayName("should save group when no overlapping age range exists")
        void shouldSaveGroupWhenNoOverlappingRangeExists() {
            AgeRange newRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Juniors", OWNER, newRange);
            TrainingGroup expected = TrainingGroup.create(command);
            when(userGroupRepository.findAllTrainingGroups()).thenReturn(List.of());
            when(userGroupRepository.save(any())).thenReturn(expected);

            TrainingGroup result = service.createTrainingGroup(command);

            assertThat(result).isNotNull();
            verify(userGroupRepository).save(any(TrainingGroup.class));
        }

        @Test
        @DisplayName("should throw OverlappingAgeRangeException when existing group has overlapping range")
        void shouldThrowWhenAgeRangeOverlapsExistingGroup() {
            AgeRange existingRange = new AgeRange(10, 18);
            TrainingGroup existingGroup = TrainingGroup.reconstruct(
                    GROUP_ID, "Existing Group", Set.of(OWNER), Set.of(), existingRange, null);
            when(userGroupRepository.findAllTrainingGroups()).thenReturn(List.of(existingGroup));

            AgeRange overlappingRange = new AgeRange(15, 25);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("New Group", OWNER, overlappingRange);

            assertThatThrownBy(() -> service.createTrainingGroup(command))
                    .isInstanceOf(AgeRange.OverlappingAgeRangeException.class);
        }
    }

    @Nested
    @DisplayName("updateTrainingGroupAgeRange()")
    class UpdateTrainingGroupAgeRangeMethod {

        @Test
        @DisplayName("should update age range when new range does not overlap other groups")
        void shouldUpdateAgeRangeWhenNoOverlap() {
            AgeRange currentRange = new AgeRange(10, 18);
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(OWNER), Set.of(), currentRange, null);
            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(userGroupRepository.findAllTrainingGroups()).thenReturn(List.of(group));
            when(userGroupRepository.save(any())).thenReturn(group);

            AgeRange newRange = new AgeRange(10, 16);
            TrainingGroup result = service.updateTrainingGroupAgeRange(GROUP_ID, newRange, OWNER);

            assertThat(result).isNotNull();
            verify(userGroupRepository).save(group);
        }

        @Test
        @DisplayName("should throw OverlappingAgeRangeException when new range overlaps another group")
        void shouldThrowWhenNewRangeOverlapsAnotherGroup() {
            AgeRange currentRange = new AgeRange(10, 18);
            TrainingGroup groupToUpdate = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(OWNER), Set.of(), currentRange, null);

            UserGroupId otherGroupId = new UserGroupId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
            AgeRange otherRange = new AgeRange(20, 30);
            TrainingGroup otherGroup = TrainingGroup.reconstruct(
                    otherGroupId, "Seniors", Set.of(OWNER), Set.of(), otherRange, null);

            when(userGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(groupToUpdate));
            when(userGroupRepository.findAllTrainingGroups()).thenReturn(List.of(groupToUpdate, otherGroup));

            AgeRange overlappingRange = new AgeRange(18, 25);

            assertThatThrownBy(() -> service.updateTrainingGroupAgeRange(GROUP_ID, overlappingRange, OWNER))
                    .isInstanceOf(AgeRange.OverlappingAgeRangeException.class);
        }
    }

    @Nested
    @DisplayName("listTrainingGroups()")
    class ListTrainingGroupsMethod {

        @Test
        @DisplayName("should delegate to repository and return all training groups")
        void shouldDelegateToRepository() {
            TrainingGroup group1 = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(OWNER), Set.of(), new AgeRange(10, 18), null);
            UserGroupId group2Id = new UserGroupId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
            TrainingGroup group2 = TrainingGroup.reconstruct(
                    group2Id, "Seniors", Set.of(OWNER), Set.of(), new AgeRange(19, 30), null);
            when(userGroupRepository.findAllTrainingGroups()).thenReturn(List.of(group1, group2));

            List<TrainingGroup> result = service.listTrainingGroups();

            assertThat(result).containsExactlyInAnyOrder(group1, group2);
        }

        @Test
        @DisplayName("should return empty list when no training groups exist")
        void shouldReturnEmptyListWhenNoGroups() {
            when(userGroupRepository.findAllTrainingGroups()).thenReturn(List.of());

            List<TrainingGroup> result = service.listTrainingGroups();

            assertThat(result).isEmpty();
        }
    }
}
