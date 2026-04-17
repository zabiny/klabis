package com.klabis.groups.traininggroup.application;

import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.common.patch.PatchField;
import com.klabis.common.users.UserId;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TrainingGroupManagementService")
@ExtendWith(MockitoExtension.class)
class TrainingGroupManagementServiceTest {

    private static final MemberId TRAINER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId TRAINER_2 = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final TrainingGroupId GROUP_ID = new TrainingGroupId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    @Mock
    private TrainingGroupRepository trainingGroupRepository;

    @Mock
    private ActiveMembersByAgeProvider activeMembersByAgeProvider;

    private TrainingGroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new TrainingGroupManagementService(trainingGroupRepository, activeMembersByAgeProvider);
    }

    @Nested
    @DisplayName("createTrainingGroup()")
    class CreateTrainingGroupMethod {

        @Test
        @DisplayName("should save group when no overlapping age range exists")
        void shouldSaveGroupWhenNoOverlappingRangeExists() {
            AgeRange newRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, newRange);
            TrainingGroup expected = TrainingGroup.create(command);
            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(activeMembersByAgeProvider.findActiveMemberIdsByAgeRange(anyInt(), anyInt())).thenReturn(List.of());
            when(trainingGroupRepository.save(any())).thenReturn(expected);

            TrainingGroup result = service.createTrainingGroup(command);

            assertThat(result).isNotNull();
            verify(trainingGroupRepository).save(any(TrainingGroup.class));
        }

        @Test
        @DisplayName("should create group with specified trainer")
        void shouldCreateGroupWithTrainer() {
            AgeRange newRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, newRange);

            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(activeMembersByAgeProvider.findActiveMemberIdsByAgeRange(10, 18)).thenReturn(List.of());
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTrainingGroup(command);

            ArgumentCaptor<TrainingGroup> captor = ArgumentCaptor.forClass(TrainingGroup.class);
            verify(trainingGroupRepository).save(captor.capture());
            assertThat(captor.getValue().getTrainers()).containsExactly(TRAINER);
        }

        @Test
        @DisplayName("should auto-assign existing active members matching the age range when group is created")
        void shouldAutoAssignExistingMembersMatchingAgeRange() {
            AgeRange newRange = new AgeRange(10, 18);
            MemberId matchingMember1 = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            MemberId matchingMember2 = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, newRange);

            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(activeMembersByAgeProvider.findActiveMemberIdsByAgeRange(10, 18))
                    .thenReturn(List.of(matchingMember1, matchingMember2));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTrainingGroup(command);

            ArgumentCaptor<TrainingGroup> captor = ArgumentCaptor.forClass(TrainingGroup.class);
            verify(trainingGroupRepository).save(captor.capture());
            TrainingGroup saved = captor.getValue();
            assertThat(saved.getMembers())
                    .extracting(m -> new MemberId(m.userId().uuid()))
                    .containsExactlyInAnyOrder(matchingMember1, matchingMember2);
        }

        @Test
        @DisplayName("should create group with no pre-assigned members when no active members match")
        void shouldCreateGroupWithNoMembersWhenNoMatchingActiveMembers() {
            AgeRange newRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, newRange);

            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(activeMembersByAgeProvider.findActiveMemberIdsByAgeRange(10, 18)).thenReturn(List.of());
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTrainingGroup(command);

            ArgumentCaptor<TrainingGroup> captor = ArgumentCaptor.forClass(TrainingGroup.class);
            verify(trainingGroupRepository).save(captor.capture());
            assertThat(captor.getValue().getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should throw OverlappingAgeRangeException when existing group has overlapping range")
        void shouldThrowWhenAgeRangeOverlapsExistingGroup() {
            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(true);

            AgeRange overlappingRange = new AgeRange(15, 25);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("New Group", TRAINER, overlappingRange);

            assertThatThrownBy(() -> service.createTrainingGroup(command))
                    .isInstanceOf(AgeRange.OverlappingAgeRangeException.class);
        }
    }

    @Nested
    @DisplayName("updateTrainingGroup()")
    class UpdateTrainingGroupMethod {

        @Test
        @DisplayName("should update name when name patch field is provided")
        void shouldUpdateNameWhenProvided() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.of("Seniors"),
                    PatchField.notProvided(),
                    PatchField.notProvided()
            );

            TrainingGroup result = service.updateTrainingGroup(GROUP_ID, command);

            assertThat(result.getName()).isEqualTo("Seniors");
            verify(trainingGroupRepository).save(group);
        }

        @Test
        @DisplayName("should update age range when both minAge and maxAge are provided")
        void shouldUpdateAgeRangeWhenBothProvided() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.notProvided(),
                    PatchField.of(new AgeRange(5, 12)),
                    PatchField.notProvided()
            );

            TrainingGroup result = service.updateTrainingGroup(GROUP_ID, command);

            assertThat(result.getAgeRange()).isEqualTo(new AgeRange(5, 12));
        }

        @Test
        @DisplayName("should throw OverlappingAgeRangeException when new age range overlaps another group")
        void shouldThrowWhenNewAgeRangeOverlapsAnotherGroup() {
            TrainingGroup groupToUpdate = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);

            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(groupToUpdate));
            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(true);

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.notProvided(),
                    PatchField.of(new AgeRange(18, 25)),
                    PatchField.notProvided()
            );

            assertThatThrownBy(() -> service.updateTrainingGroup(GROUP_ID, command))
                    .isInstanceOf(AgeRange.OverlappingAgeRangeException.class);
        }

        @Test
        @DisplayName("should replace trainers when trainers patch field is provided")
        void shouldReplaceTrainersWhenProvided() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.notProvided(),
                    PatchField.notProvided(),
                    PatchField.of(Set.of(TRAINER_2))
            );

            TrainingGroup result = service.updateTrainingGroup(GROUP_ID, command);

            assertThat(result.getTrainers()).containsExactly(TRAINER_2);
        }

        @Test
        @DisplayName("should apply all provided fields atomically")
        void shouldApplyAllProvidedFieldsAtomically() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.of("Updated Juniors"),
                    PatchField.of(new AgeRange(8, 14)),
                    PatchField.of(Set.of(TRAINER_2))
            );

            TrainingGroup result = service.updateTrainingGroup(GROUP_ID, command);

            assertThat(result.getName()).isEqualTo("Updated Juniors");
            assertThat(result.getAgeRange()).isEqualTo(new AgeRange(8, 14));
            assertThat(result.getTrainers()).containsExactly(TRAINER_2);
        }

        @Test
        @DisplayName("should leave group unchanged when no fields are provided")
        void shouldLeaveGroupUnchangedWhenNoFieldsProvided() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.notProvided(),
                    PatchField.notProvided(),
                    PatchField.notProvided()
            );

            TrainingGroup result = service.updateTrainingGroup(GROUP_ID, command);

            assertThat(result.getName()).isEqualTo("Juniors");
            assertThat(result.getAgeRange()).isEqualTo(new AgeRange(10, 18));
            assertThat(result.getTrainers()).containsExactly(TRAINER);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when training group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.of("New Name"),
                    PatchField.notProvided(),
                    PatchField.notProvided()
            );

            assertThatThrownBy(() -> service.updateTrainingGroup(GROUP_ID, command))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteTrainingGroup()")
    class DeleteTrainingGroupMethod {

        @Test
        @DisplayName("should delete group when it exists")
        void shouldDeleteGroupWhenExists() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

            service.deleteTrainingGroup(GROUP_ID);

            verify(trainingGroupRepository).delete(GROUP_ID);
        }

        @Test
        @DisplayName("should throw GroupNotFoundException when training group does not exist")
        void shouldThrowWhenGroupNotFound() {
            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteTrainingGroup(GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addMemberToTrainingGroup() — manual path")
    class AddMemberToTrainingGroupMethod {

        private static final MemberId MEMBER = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        private static final TrainingGroupId OTHER_GROUP_ID = new TrainingGroupId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));

        @Test
        @DisplayName("7.1 should throw MemberAlreadyInTrainingGroupException when member is already a trainee of another training group")
        void shouldThrowWhenMemberIsAlreadyTraineeOfAnotherGroup() {
            TrainingGroup conflictingGroup = TrainingGroup.reconstruct(
                    OTHER_GROUP_ID, "Seniors", Set.of(TRAINER),
                    Set.of(new GroupMembership(new UserId(MEMBER.uuid()), Instant.now())),
                    new AgeRange(19, 30), null);

            when(trainingGroupRepository.findOne(any(TrainingGroupFilter.class))).thenReturn(Optional.of(conflictingGroup));

            assertThatThrownBy(() -> service.addMemberToTrainingGroup(GROUP_ID, MEMBER))
                    .isInstanceOf(MemberAlreadyInTrainingGroupException.class);
        }

        @Test
        @DisplayName("7.2 should succeed when member is not a trainee of any training group")
        void shouldSucceedWhenMemberIsNotTraineeAnywhere() {
            TrainingGroup targetGroup = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);

            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(targetGroup));
            when(trainingGroupRepository.findOne(any(TrainingGroupFilter.class))).thenReturn(Optional.empty());
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addMemberToTrainingGroup(GROUP_ID, MEMBER);

            verify(trainingGroupRepository).save(any(TrainingGroup.class));
        }

        @Test
        @DisplayName("7.3 should succeed when member is a trainer of another training group but not a trainee anywhere")
        void shouldSucceedWhenMemberIsOnlyTrainerElsewhere() {
            // MEMBER is a trainer (owner) of OTHER_GROUP — findGroupForMember queries the members table (trainees only)
            // so it returns empty, meaning the trainer exemption works automatically via the existing query semantics
            TrainingGroup targetGroup = TrainingGroup.reconstruct(
                    GROUP_ID, "Juniors", Set.of(TRAINER), Set.of(), new AgeRange(10, 18), null);

            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(targetGroup));
            when(trainingGroupRepository.findOne(any(TrainingGroupFilter.class))).thenReturn(Optional.empty());
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addMemberToTrainingGroup(GROUP_ID, MEMBER);

            verify(trainingGroupRepository).save(any(TrainingGroup.class));
        }

        @Test
        @DisplayName("7.4 automatic assignEligibleMember path during createTrainingGroup is NOT checked — auto-assign still runs without exclusivity guard")
        void shouldNotApplyExclusivityCheckOnAutoAssignPath() {
            // Auto-assign path: createTrainingGroup calls assignEligibleMember in a loop, not addMemberToTrainingGroup.
            // findGroupForMember must NOT be called during createTrainingGroup.
            AgeRange newRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, newRange);

            when(trainingGroupRepository.exists(any(TrainingGroupFilter.class))).thenReturn(false);
            when(activeMembersByAgeProvider.findActiveMemberIdsByAgeRange(10, 18))
                    .thenReturn(List.of(MEMBER));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTrainingGroup(command);

            // findOne(filter.withMemberIs(...)) must never be called during the automatic path
            org.mockito.Mockito.verify(trainingGroupRepository, org.mockito.Mockito.never())
                    .findOne(any(TrainingGroupFilter.class));
        }
    }
}
