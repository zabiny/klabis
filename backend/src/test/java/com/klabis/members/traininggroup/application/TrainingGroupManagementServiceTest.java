package com.klabis.members.traininggroup.application;

import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.common.patch.PatchField;
import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
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
            when(trainingGroupRepository.findAll()).thenReturn(List.of());
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

            when(trainingGroupRepository.findAll()).thenReturn(List.of());
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

            when(trainingGroupRepository.findAll()).thenReturn(List.of());
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

            when(trainingGroupRepository.findAll()).thenReturn(List.of());
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
            AgeRange existingRange = new AgeRange(10, 18);
            TrainingGroup existingGroup = TrainingGroup.reconstruct(
                    GROUP_ID, "Existing Group", Set.of(TRAINER), Set.of(), existingRange, null);
            when(trainingGroupRepository.findAll()).thenReturn(List.of(existingGroup));

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
            when(trainingGroupRepository.findAll()).thenReturn(List.of(group));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.notProvided(),
                    PatchField.of(5),
                    PatchField.of(12),
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

            TrainingGroupId otherGroupId = new TrainingGroupId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
            TrainingGroup otherGroup = TrainingGroup.reconstruct(
                    otherGroupId, "Seniors", Set.of(TRAINER), Set.of(), new AgeRange(20, 30), null);

            when(trainingGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(groupToUpdate));
            when(trainingGroupRepository.findAll()).thenReturn(List.of(groupToUpdate, otherGroup));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.notProvided(),
                    PatchField.of(18),
                    PatchField.of(25),
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
            when(trainingGroupRepository.findAll()).thenReturn(List.of(group));
            when(trainingGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                    PatchField.of("Updated Juniors"),
                    PatchField.of(8),
                    PatchField.of(14),
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
}
