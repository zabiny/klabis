package com.klabis.members.traininggroup.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.members.MemberAssignedToTrainingGroupEvent;
import com.klabis.members.MemberId;
import com.klabis.groups.common.domain.AgeRangeOverlap;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrainingGroup JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(TrainingGroupPersistenceTest.DomainEventCapturingConfig.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'TEST001', 'Trainer', 'Member', '1985-01-01', 'CZ', 'MALE', 'trainer@example.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'TEST002', 'Regular', 'Member', '2010-01-01', 'CZ', 'MALE', 'regular@example.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class TrainingGroupPersistenceTest {

    @TestConfiguration
    static class DomainEventCapturingConfig {
        @Bean
        DomainEventCapture domainEventCapture() {
            return new DomainEventCapture();
        }
    }

    static class DomainEventCapture {
        private final List<Object> capturedEvents = new ArrayList<>();

        @EventListener
        public void onEvent(MemberAssignedToTrainingGroupEvent event) {
            capturedEvents.add(event);
        }

        public List<Object> getCapturedEvents() {
            return List.copyOf(capturedEvents);
        }

        public void clear() {
            capturedEvents.clear();
        }
    }

    @Autowired
    private TrainingGroupRepository trainingGroupRepository;

    @Autowired
    private DomainEventCapture domainEventCapture;

    private static final MemberId TRAINER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId REGULAR_MEMBER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @BeforeEach
    void clearEvents() {
        domainEventCapture.clear();
    }

    @Nested
    @DisplayName("save() and findById() — round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should save and retrieve training group with trainer")
        void shouldSaveAndRetrieveTrainingGroup() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Junior Sprint", TRAINER, new AgeRange(10, 18)));

            TrainingGroup saved = trainingGroupRepository.save(group);
            Optional<TrainingGroup> found = trainingGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            TrainingGroup retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Junior Sprint");
            assertThat(retrieved.getTrainers()).containsExactly(TRAINER);
            assertThat(retrieved.getAgeRange()).isEqualTo(new AgeRange(10, 18));
            assertThat(retrieved.getAuditMetadata()).isNotNull();
        }

        @Test
        @DisplayName("should save and retrieve training group with a member")
        void shouldSaveAndRetrieveTrainingGroupWithMember() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Junior Sprint", TRAINER, new AgeRange(10, 18)));
            group.assignEligibleMember(REGULAR_MEMBER);
            group.clearDomainEvents();

            TrainingGroup saved = trainingGroupRepository.save(group);
            Optional<TrainingGroup> found = trainingGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().hasMember(REGULAR_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return empty when group not found")
        void shouldReturnEmptyWhenGroupNotFound() {
            Optional<TrainingGroup> found = trainingGroupRepository.findById(new TrainingGroupId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should persist audit metadata after save")
        void shouldPersistAuditMetadataAfterSave() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Audit Test Group", TRAINER, new AgeRange(8, 14)));

            TrainingGroup saved = trainingGroupRepository.save(group);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should preserve age range through round-trip")
        void shouldPreserveAgeRangeThroughRoundTrip() {
            AgeRange ageRange = new AgeRange(14, 21);
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Youth Group", TRAINER, ageRange));

            TrainingGroup saved = trainingGroupRepository.save(group);
            TrainingGroup retrieved = trainingGroupRepository.findById(saved.getId()).orElseThrow();

            assertThat(retrieved.getAgeRange()).isEqualTo(ageRange);
        }
    }

    @Nested
    @DisplayName("findAll(TrainingGroupFilter)")
    class FindAll {

        @Test
        @DisplayName("should return all saved training groups when filter is all()")
        void shouldReturnAllGroups() {
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14))));
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Seniors", TRAINER, new AgeRange(18, 40))));

            List<TrainingGroup> result = trainingGroupRepository.findAll(TrainingGroupFilter.all());

            assertThat(result).hasSize(2);
            assertThat(result).extracting(TrainingGroup::getName)
                    .containsExactlyInAnyOrder("Juniors", "Seniors");
        }

        @Test
        @DisplayName("should return empty list when no groups exist")
        void shouldReturnEmptyListWhenNoGroups() {
            List<TrainingGroup> result = trainingGroupRepository.findAll(TrainingGroupFilter.all());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return only groups where given member is a trainer when withTrainerIs filter is used")
        void shouldReturnGroupsWhereMemberIsTrainer() {
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14))));
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Seniors", TRAINER, new AgeRange(18, 40))));

            List<TrainingGroup> result = trainingGroupRepository.findAll(
                    TrainingGroupFilter.all().withTrainerIs(TRAINER));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(TrainingGroup::getName)
                    .containsExactlyInAnyOrder("Juniors", "Seniors");
        }

        @Test
        @DisplayName("should return empty list when member is not a trainer in any group")
        void shouldReturnEmptyWhenNotATrainer() {
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14))));

            List<TrainingGroup> result = trainingGroupRepository.findAll(
                    TrainingGroupFilter.all().withTrainerIs(REGULAR_MEMBER));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOne(TrainingGroupFilter)")
    class FindOne {

        @Test
        @DisplayName("should return the training group the member is assigned to when withMemberIs filter is used")
        void shouldReturnGroupForMember() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14)));
            group.assignEligibleMember(REGULAR_MEMBER);
            group.clearDomainEvents();
            trainingGroupRepository.save(group);

            Optional<TrainingGroup> result = trainingGroupRepository.findOne(
                    TrainingGroupFilter.all().withMemberIs(REGULAR_MEMBER));

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Juniors");
        }

        @Test
        @DisplayName("should return empty when member is not in any training group")
        void shouldReturnEmptyWhenMemberNotInAnyGroup() {
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14))));

            Optional<TrainingGroup> result = trainingGroupRepository.findOne(
                    TrainingGroupFilter.all().withMemberIs(REGULAR_MEMBER));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOne(filter) invariant — throws when multiple rows match")
    class FindOneContract {

        @Test
        @DisplayName("should throw IllegalStateException when withMemberIs matches more than one group (invariant violation)")
        void shouldThrowWhenMemberAppearsInTwoGroups() {
            TrainingGroup group1 = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Group A", TRAINER, new AgeRange(8, 14)));
            group1.assignEligibleMember(REGULAR_MEMBER);
            group1.clearDomainEvents();
            trainingGroupRepository.save(group1);

            TrainingGroup group2 = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Group B", TRAINER, new AgeRange(15, 25)));
            group2.assignEligibleMember(REGULAR_MEMBER);
            group2.clearDomainEvents();
            trainingGroupRepository.save(group2);

            assertThatThrownBy(() ->
                    trainingGroupRepository.findOne(TrainingGroupFilter.all().withMemberIs(REGULAR_MEMBER)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("findOne expected at most 1 result");
        }
    }

    @Nested
    @DisplayName("exists(TrainingGroupFilter) — overlap check")
    class ExistsOverlap {

        @Test
        @DisplayName("should detect overlap when another group covers the same age range")
        void shouldDetectOverlappingAgeRange() {
            trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14))));

            boolean result = trainingGroupRepository.exists(
                    TrainingGroupFilter.all().withOverlap(new AgeRangeOverlap(new AgeRange(10, 18), null)));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should not detect overlap when excludeId is the only matching group")
        void shouldNotDetectOverlapWhenExcluded() {
            TrainingGroup saved = trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(8, 14))));

            boolean result = trainingGroupRepository.exists(
                    TrainingGroupFilter.all().withOverlap(new AgeRangeOverlap(new AgeRange(8, 14), saved.getId())));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when no groups exist")
        void shouldReturnFalseWhenNoGroups() {
            boolean result = trainingGroupRepository.exists(
                    TrainingGroupFilter.all().withOverlap(new AgeRangeOverlap(new AgeRange(10, 18), null)));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteMethod {

        @Test
        @DisplayName("should delete group so it can no longer be found")
        void shouldDeleteGroup() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("To Be Deleted", TRAINER, new AgeRange(10, 18)));
            TrainingGroup saved = trainingGroupRepository.save(group);
            TrainingGroupId id = saved.getId();

            trainingGroupRepository.delete(id);

            assertThat(trainingGroupRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Domain event publication")
    class DomainEventPublication {

        @Test
        @DisplayName("should publish MemberAssignedToTrainingGroupEvent when saving group after assignEligibleMember")
        void shouldPublishEventWhenSavingTrainingGroupAfterAssignEligibleMember() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(10, 18)));
            group.assignEligibleMember(REGULAR_MEMBER);

            trainingGroupRepository.save(group);

            assertThat(domainEventCapture.getCapturedEvents()).hasSize(1);
            MemberAssignedToTrainingGroupEvent event =
                    (MemberAssignedToTrainingGroupEvent) domainEventCapture.getCapturedEvents().get(0);
            assertThat(event.memberId()).isEqualTo(REGULAR_MEMBER);
            assertThat(event.groupName()).isEqualTo("Juniors");
        }

        @Test
        @DisplayName("should not publish any event when saving group without member changes")
        void shouldNotPublishEventWhenNoMemberChanges() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Seniors", TRAINER, new AgeRange(18, 40)));

            trainingGroupRepository.save(group);

            assertThat(domainEventCapture.getCapturedEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("update — trainer management")
    class UpdateTrainers {

        @Test
        @DisplayName("should persist added trainer after save")
        void shouldPersistAddedTrainer() {
            TrainingGroup group = trainingGroupRepository.save(TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", TRAINER, new AgeRange(10, 18))));
            MemberId anotherTrainer = REGULAR_MEMBER;

            group.addTrainer(anotherTrainer);
            trainingGroupRepository.save(group);

            TrainingGroup retrieved = trainingGroupRepository.findById(group.getId()).orElseThrow();
            assertThat(retrieved.getTrainers()).containsExactlyInAnyOrder(TRAINER, anotherTrainer);
        }
    }
}
