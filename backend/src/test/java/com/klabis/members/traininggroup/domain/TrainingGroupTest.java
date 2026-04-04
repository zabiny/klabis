package com.klabis.members.traininggroup.domain;

import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.members.MemberAssignedToTrainingGroupEvent;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrainingGroup domain unit tests")
class TrainingGroupTest {

    private static final MemberId TRAINER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId TRAINER_2 = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId TRAINER_3 = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final MemberId REGULAR_MEMBER = new MemberId(UUID.fromString("44444444-4444-4444-4444-444444444444"));

    @Nested
    @DisplayName("TrainingGroup.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with correct name, trainer, and age range")
        void shouldCreateGroupWithCorrectProperties() {
            AgeRange ageRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Junior Sprint", TRAINER, ageRange);

            TrainingGroup group = TrainingGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Junior Sprint");
            assertThat(group.getTrainers()).containsExactly(TRAINER);
            assertThat(group.getAgeRange()).isEqualTo(ageRange);
            assertThat(group.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should generate unique IDs for different groups")
        void shouldGenerateUniqueIds() {
            AgeRange ageRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Junior Sprint", TRAINER, ageRange);

            TrainingGroup group1 = TrainingGroup.create(command);
            TrainingGroup group2 = TrainingGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            AgeRange ageRange = new AgeRange(10, 18);
            assertThatThrownBy(() -> new TrainingGroup.CreateTrainingGroup("", TRAINER, ageRange))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null trainer")
        void shouldRejectNullTrainer() {
            AgeRange ageRange = new AgeRange(10, 18);
            assertThatThrownBy(() -> new TrainingGroup.CreateTrainingGroup("Junior Sprint", null, ageRange))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null age range")
        void shouldRejectNullAgeRange() {
            assertThatThrownBy(() -> new TrainingGroup.CreateTrainingGroup("Junior Sprint", TRAINER, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("matchesByAge()")
    class MatchesByAgeMethod {

        private final AgeRange ageRange = new AgeRange(10, 18);
        private final TrainingGroup group = TrainingGroup.create(
                new TrainingGroup.CreateTrainingGroup("Junior Sprint", TRAINER, ageRange));

        @Test
        @DisplayName("should return true when member age falls within the age range")
        void shouldReturnTrueWhenAgeIsWithinRange() {
            assertThat(group.matchesByAge(LocalDate.now().minusYears(15))).isTrue();
        }

        @Test
        @DisplayName("should return true when member age is exactly at minimum boundary")
        void shouldReturnTrueAtMinAgeBoundary() {
            assertThat(group.matchesByAge(LocalDate.now().minusYears(10))).isTrue();
        }

        @Test
        @DisplayName("should return true when member age is exactly at maximum boundary")
        void shouldReturnTrueAtMaxAgeBoundary() {
            assertThat(group.matchesByAge(LocalDate.now().minusYears(18))).isTrue();
        }

        @Test
        @DisplayName("should return false when member is too young")
        void shouldReturnFalseWhenMemberIsTooYoung() {
            assertThat(group.matchesByAge(LocalDate.now().minusYears(9))).isFalse();
        }

        @Test
        @DisplayName("should return false when member is too old")
        void shouldReturnFalseWhenMemberIsTooOld() {
            assertThat(group.matchesByAge(LocalDate.now().minusYears(19))).isFalse();
        }

        @Test
        @DisplayName("should reject null date of birth")
        void shouldRejectNullDateOfBirth() {
            assertThatThrownBy(() -> group.matchesByAge(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Trainer methods (alias for owner operations)")
    class TrainerMethods {

        private TrainingGroup group;

        @BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("getTrainers() should return current trainers")
        void shouldReturnTrainers() {
            assertThat(group.getTrainers()).containsExactly(TRAINER);
        }

        @Test
        @DisplayName("addTrainer() should add trainer to group")
        void shouldAddTrainer() {
            group.addTrainer(TRAINER_2);

            assertThat(group.getTrainers()).containsExactlyInAnyOrder(TRAINER, TRAINER_2);
        }

        @Test
        @DisplayName("removeTrainer() should remove trainer from group")
        void shouldRemoveTrainer() {
            group.addTrainer(TRAINER_2);
            group.removeTrainer(TRAINER);

            assertThat(group.getTrainers()).containsExactly(TRAINER_2);
        }

        @Test
        @DisplayName("removeTrainer() should throw when removing last trainer")
        void shouldThrowWhenRemovingLastTrainer() {
            assertThatThrownBy(() -> group.removeTrainer(TRAINER))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("replaceTrainers() should replace all trainers with new set")
        void shouldReplaceAllTrainers() {
            group.replaceTrainers(Set.of(TRAINER_2, TRAINER_3));

            assertThat(group.getTrainers()).containsExactlyInAnyOrder(TRAINER_2, TRAINER_3);
        }

        @Test
        @DisplayName("replaceTrainers() should throw when given empty set")
        void shouldThrowWhenReplacingWithEmptySet() {
            assertThatThrownBy(() -> group.replaceTrainers(Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("addTrainer() should throw on null trainer")
        void shouldThrowOnNullAddTrainer() {
            assertThatThrownBy(() -> group.addTrainer(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("removeTrainer() should throw on null trainer")
        void shouldThrowOnNullRemoveTrainer() {
            assertThatThrownBy(() -> group.removeTrainer(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateAgeRange()")
    class UpdateAgeRangeMethod {

        private TrainingGroup group;

        @BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("should update age range")
        void shouldUpdateAgeRange() {
            AgeRange newRange = new AgeRange(12, 20);

            group.updateAgeRange(newRange);

            assertThat(group.getAgeRange()).isEqualTo(newRange);
        }

        @Test
        @DisplayName("should throw on null age range")
        void shouldThrowOnNullAgeRange() {
            assertThatThrownBy(() -> group.updateAgeRange(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addMember() and assignEligibleMember() — domain event registration")
    class MemberEventRegistration {

        private TrainingGroup group;

        @BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("addMember() should register MemberAssignedToTrainingGroupEvent")
        void shouldRegisterEventOnAddMember() {
            group.addMember(REGULAR_MEMBER);

            assertThat(group.getDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(MemberAssignedToTrainingGroupEvent.class);

            MemberAssignedToTrainingGroupEvent event =
                    (MemberAssignedToTrainingGroupEvent) group.getDomainEvents().get(0);
            assertThat(event.memberId()).isEqualTo(REGULAR_MEMBER);
            assertThat(event.groupId()).isEqualTo(group.getId());
            assertThat(event.groupName()).isEqualTo("Juniors");
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("assignEligibleMember() should register MemberAssignedToTrainingGroupEvent")
        void shouldRegisterEventOnAssignEligibleMember() {
            group.assignEligibleMember(REGULAR_MEMBER);

            assertThat(group.getDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(MemberAssignedToTrainingGroupEvent.class);

            MemberAssignedToTrainingGroupEvent event =
                    (MemberAssignedToTrainingGroupEvent) group.getDomainEvents().get(0);
            assertThat(event.memberId()).isEqualTo(REGULAR_MEMBER);
            assertThat(event.groupId()).isEqualTo(group.getId());
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberMethod {

        private TrainingGroup group;

        @BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);
            group.assignEligibleMember(REGULAR_MEMBER);
            group.clearDomainEvents();
        }

        @Test
        @DisplayName("should remove member from group")
        void shouldRemoveMember() {
            group.removeMember(REGULAR_MEMBER);

            assertThat(group.hasMember(REGULAR_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should not register any domain event on removal")
        void shouldNotRegisterEventOnRemoval() {
            group.removeMember(REGULAR_MEMBER);

            assertThat(group.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("removeMember() should throw on null memberId")
        void shouldThrowOnNullRemoveMember() {
            assertThatThrownBy(() -> group.removeMember(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isLastTrainer()")
    class IsLastTrainerMethod {

        @Test
        @DisplayName("should return true when member is the sole trainer")
        void shouldReturnTrueWhenSoleTrainer() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);

            assertThat(group.isLastTrainer(TRAINER)).isTrue();
        }

        @Test
        @DisplayName("should return false when there are multiple trainers")
        void shouldReturnFalseWhenMultipleTrainers() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER, TRAINER_2), Set.of(),
                    new AgeRange(10, 18), null);

            assertThat(group.isLastTrainer(TRAINER)).isFalse();
        }

        @Test
        @DisplayName("should return false when member is not a trainer at all")
        void shouldReturnFalseWhenNotATrainer() {
            TrainingGroup group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);

            assertThat(group.isLastTrainer(TRAINER_2)).isFalse();
        }
    }

    @Nested
    @DisplayName("addMember() null guard")
    class AddMemberNullGuard {

        private TrainingGroup group;

        @BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new TrainingGroupId(UUID.randomUUID()), "Juniors", Set.of(TRAINER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("addMember() should throw on null memberId")
        void shouldThrowOnNullAddMember() {
            assertThatThrownBy(() -> group.addMember(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
