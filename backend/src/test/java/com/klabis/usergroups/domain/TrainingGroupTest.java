package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import com.klabis.usergroups.MemberAssignedToTrainingGroupEvent;
import com.klabis.usergroups.UserGroupId;
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

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    @Nested
    @DisplayName("AgeRange")
    class AgeRangeTests {

        @Nested
        @DisplayName("constructor validation")
        class ConstructorValidation {

            @Test
            @DisplayName("should reject negative minAge")
            void shouldRejectNegativeMinAge() {
                assertThatThrownBy(() -> new AgeRange(-1, 10))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("should reject maxAge less than minAge")
            void shouldRejectMaxAgeLessThanMinAge() {
                assertThatThrownBy(() -> new AgeRange(10, 5))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("should allow equal minAge and maxAge")
            void shouldAllowEqualMinAndMax() {
                AgeRange range = new AgeRange(10, 10);
                assertThat(range.minAge()).isEqualTo(10);
                assertThat(range.maxAge()).isEqualTo(10);
            }

            @Test
            @DisplayName("should allow zero minAge")
            void shouldAllowZeroMinAge() {
                AgeRange range = new AgeRange(0, 5);
                assertThat(range.minAge()).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("includes()")
        class IncludesMethod {

            private final AgeRange range = new AgeRange(10, 20);

            @Test
            @DisplayName("should return true for age at minimum boundary")
            void shouldReturnTrueAtMinBoundary() {
                assertThat(range.includes(10)).isTrue();
            }

            @Test
            @DisplayName("should return true for age at maximum boundary")
            void shouldReturnTrueAtMaxBoundary() {
                assertThat(range.includes(20)).isTrue();
            }

            @Test
            @DisplayName("should return true for age within range")
            void shouldReturnTrueForAgeWithinRange() {
                assertThat(range.includes(15)).isTrue();
            }

            @Test
            @DisplayName("should return false for age below minimum")
            void shouldReturnFalseForAgeBelowMin() {
                assertThat(range.includes(9)).isFalse();
            }

            @Test
            @DisplayName("should return false for age above maximum")
            void shouldReturnFalseForAgeAboveMax() {
                assertThat(range.includes(21)).isFalse();
            }
        }

        @Nested
        @DisplayName("overlaps()")
        class OverlapsMethod {

            private final AgeRange base = new AgeRange(10, 20);

            @Test
            @DisplayName("should return false when other range is entirely below")
            void shouldReturnFalseWhenOtherIsEntirelyBelow() {
                AgeRange below = new AgeRange(0, 9);
                assertThat(base.overlaps(below)).isFalse();
            }

            @Test
            @DisplayName("should return false when other range is entirely above")
            void shouldReturnFalseWhenOtherIsEntirelyAbove() {
                AgeRange above = new AgeRange(21, 30);
                assertThat(base.overlaps(above)).isFalse();
            }

            @Test
            @DisplayName("should return true when other range fully contains this range")
            void shouldReturnTrueWhenFullyContained() {
                AgeRange container = new AgeRange(5, 25);
                assertThat(base.overlaps(container)).isTrue();
            }

            @Test
            @DisplayName("should return true when this range fully contains other range")
            void shouldReturnTrueWhenContainsOther() {
                AgeRange inner = new AgeRange(12, 18);
                assertThat(base.overlaps(inner)).isTrue();
            }

            @Test
            @DisplayName("should return true when other range partially overlaps from below")
            void shouldReturnTrueWhenPartialOverlapFromBelow() {
                AgeRange partialBelow = new AgeRange(5, 15);
                assertThat(base.overlaps(partialBelow)).isTrue();
            }

            @Test
            @DisplayName("should return true when other range partially overlaps from above")
            void shouldReturnTrueWhenPartialOverlapFromAbove() {
                AgeRange partialAbove = new AgeRange(15, 25);
                assertThat(base.overlaps(partialAbove)).isTrue();
            }

            @Test
            @DisplayName("should return true when ranges share only min boundary")
            void shouldReturnTrueWhenAdjacentAtMin() {
                AgeRange adjacentBelow = new AgeRange(0, 10);
                assertThat(base.overlaps(adjacentBelow)).isTrue();
            }

            @Test
            @DisplayName("should return true when ranges share only max boundary")
            void shouldReturnTrueWhenAdjacentAtMax() {
                AgeRange adjacentAbove = new AgeRange(20, 30);
                assertThat(base.overlaps(adjacentAbove)).isTrue();
            }

            @Test
            @DisplayName("should return false when ranges are exactly adjacent without sharing a boundary")
            void shouldReturnFalseForStrictlyAdjacentRanges() {
                AgeRange strictlyBelow = new AgeRange(0, 9);
                AgeRange strictlyAbove = new AgeRange(21, 30);
                assertThat(base.overlaps(strictlyBelow)).isFalse();
                assertThat(base.overlaps(strictlyAbove)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("TrainingGroup.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with correct name, trainer, and age range")
        void shouldCreateGroupWithCorrectProperties() {
            AgeRange ageRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Junior Sprint", OWNER, ageRange);

            TrainingGroup group = TrainingGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Junior Sprint");
            assertThat(group.getTrainers()).containsExactly(OWNER);
            assertThat(group.getAgeRange()).isEqualTo(ageRange);
            assertThat(group.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should generate unique IDs for different groups")
        void shouldGenerateUniqueIds() {
            AgeRange ageRange = new AgeRange(10, 18);
            TrainingGroup.CreateTrainingGroup command =
                    new TrainingGroup.CreateTrainingGroup("Junior Sprint", OWNER, ageRange);

            TrainingGroup group1 = TrainingGroup.create(command);
            TrainingGroup group2 = TrainingGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            AgeRange ageRange = new AgeRange(10, 18);
            assertThatThrownBy(() -> new TrainingGroup.CreateTrainingGroup("", OWNER, ageRange))
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
            assertThatThrownBy(() -> new TrainingGroup.CreateTrainingGroup("Junior Sprint", OWNER, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("matchesByAge()")
    class MatchesByAgeMethod {

        private final AgeRange ageRange = new AgeRange(10, 18);
        private final TrainingGroup group = TrainingGroup.create(
                new TrainingGroup.CreateTrainingGroup("Junior Sprint", OWNER, ageRange));

        @Test
        @DisplayName("should return true when member age falls within the age range")
        void shouldReturnTrueWhenAgeIsWithinRange() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(15);

            assertThat(group.matchesByAge(dateOfBirth)).isTrue();
        }

        @Test
        @DisplayName("should return true when member age is exactly at minimum boundary")
        void shouldReturnTrueAtMinAgeBoundary() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(10);

            assertThat(group.matchesByAge(dateOfBirth)).isTrue();
        }

        @Test
        @DisplayName("should return true when member age is exactly at maximum boundary")
        void shouldReturnTrueAtMaxAgeBoundary() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(18);

            assertThat(group.matchesByAge(dateOfBirth)).isTrue();
        }

        @Test
        @DisplayName("should return false when member is too young")
        void shouldReturnFalseWhenMemberIsTooYoung() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(9);

            assertThat(group.matchesByAge(dateOfBirth)).isFalse();
        }

        @Test
        @DisplayName("should return false when member is too old")
        void shouldReturnFalseWhenMemberIsTooOld() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(19);

            assertThat(group.matchesByAge(dateOfBirth)).isFalse();
        }

        @Test
        @DisplayName("should reject null date of birth")
        void shouldRejectNullDateOfBirth() {
            assertThatThrownBy(() -> group.matchesByAge(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateAgeRange(UpdateAgeRange command)")
    class UpdateAgeRangeCommandMethod {

        private static final MemberId NON_OWNER = new MemberId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

        private TrainingGroup group;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new UserGroupId(UUID.randomUUID()), "Juniors", Set.of(OWNER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("should update age range when requesting member is owner")
        void shouldUpdateAgeRangeWhenOwner() {
            AgeRange newRange = new AgeRange(12, 20);
            TrainingGroup.UpdateAgeRange command = new TrainingGroup.UpdateAgeRange(OWNER, newRange);

            group.updateAgeRange(command);

            assertThat(group.getAgeRange()).isEqualTo(newRange);
        }

        @Test
        @DisplayName("should throw NotGroupOwnerException when requesting member is not owner")
        void shouldThrowWhenNotOwner() {
            AgeRange newRange = new AgeRange(12, 20);
            TrainingGroup.UpdateAgeRange command = new TrainingGroup.UpdateAgeRange(NON_OWNER, newRange);

            assertThatThrownBy(() -> group.updateAgeRange(command))
                    .isInstanceOf(NotGroupOwnerException.class);
        }
    }

    @Nested
    @DisplayName("Trainer methods (alias for owner operations)")
    class TrainerMethods {

        private static final MemberId SECOND_TRAINER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        private static final MemberId THIRD_TRAINER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        private TrainingGroup group;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new UserGroupId(UUID.randomUUID()), "Juniors", Set.of(OWNER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("getTrainers() should return current owners")
        void shouldReturnTrainersAsOwners() {
            assertThat(group.getTrainers()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("addTrainer() should add trainer to group")
        void shouldAddTrainer() {
            group.addTrainer(SECOND_TRAINER);

            assertThat(group.getTrainers()).containsExactlyInAnyOrder(OWNER, SECOND_TRAINER);
        }

        @Test
        @DisplayName("removeTrainer() should remove trainer from group")
        void shouldRemoveTrainer() {
            group.addTrainer(SECOND_TRAINER);
            group.removeTrainer(OWNER);

            assertThat(group.getTrainers()).containsExactly(SECOND_TRAINER);
        }

        @Test
        @DisplayName("removeTrainer() should throw when removing last trainer")
        void shouldThrowWhenRemovingLastTrainer() {
            assertThatThrownBy(() -> group.removeTrainer(OWNER))
                    .isInstanceOf(UserGroup.CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("replaceTrainers() should replace all trainers with new set")
        void shouldReplaceAllTrainers() {
            Set<MemberId> newTrainers = Set.of(SECOND_TRAINER, THIRD_TRAINER);

            group.replaceTrainers(newTrainers);

            assertThat(group.getTrainers()).containsExactlyInAnyOrder(SECOND_TRAINER, THIRD_TRAINER);
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
    @DisplayName("addMember() — domain event registration")
    class AddMemberEvent {

        private static final MemberId REGULAR_MEMBER = new MemberId(UUID.fromString("44444444-4444-4444-4444-444444444444"));

        private TrainingGroup group;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            group = TrainingGroup.reconstruct(
                    new UserGroupId(UUID.randomUUID()), "Juniors", Set.of(OWNER), Set.of(),
                    new AgeRange(10, 18), null);
        }

        @Test
        @DisplayName("addMember() should register MemberAssignedToTrainingGroupEvent")
        void shouldRegisterEventOnAddMember() {
            UserGroup.AddMember command = new UserGroup.AddMember(OWNER, REGULAR_MEMBER);

            group.addMember(command);

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

        @Test
        @DisplayName("addMember() should throw when requesting member is not a trainer")
        void shouldThrowWhenNotTrainer() {
            MemberId nonTrainer = new MemberId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
            UserGroup.AddMember command = new UserGroup.AddMember(nonTrainer, REGULAR_MEMBER);

            assertThatThrownBy(() -> group.addMember(command))
                    .isInstanceOf(NotGroupOwnerException.class);
        }
    }

    private TrainingGroup buildGroup(UUID groupUuid, String name, AgeRange ageRange) {
        return TrainingGroup.reconstruct(
                new UserGroupId(groupUuid), name, Set.of(OWNER), Set.of(), ageRange, null);
    }
}
