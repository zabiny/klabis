package com.klabis.usergroups.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.members.MemberId;
import com.klabis.usergroups.MemberAssignedToTrainingGroupEvent;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.GroupType;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.ddd.annotation.Repository;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserGroup JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(UserGroupPersistenceTest.DomainEventCapturingConfig.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "classpath:/db/cleanup-user-groups.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'TEST001', 'Owner', 'Member', '2000-01-01', 'CZ', 'MALE', 'owner@example.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'TEST002', 'Extra', 'Member', '2000-01-01', 'CZ', 'MALE', 'extra@example.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class UserGroupPersistenceTest {

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
    private UserGroupRepository userGroupRepository;

    @Autowired
    private DomainEventCapture domainEventCapture;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXTRA_MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final MemberId OWNER = new MemberId(OWNER_ID);
    private static final MemberId EXTRA_MEMBER = new MemberId(EXTRA_MEMBER_ID);

    @Nested
    @DisplayName("save() and findById() — round-trip for FreeGroup")
    class SaveAndFindById {

        @Test
        @DisplayName("should save and retrieve FreeGroup with owner as member")
        void shouldSaveAndRetrieveFreeGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Trail Runners", OWNER));

            UserGroup saved = userGroupRepository.save(group);
            Optional<UserGroup> found = userGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            UserGroup retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Trail Runners");
            assertThat(retrieved.getOwners()).containsExactly(OWNER);
            assertThat(retrieved.hasMember(OWNER)).isTrue();
            assertThat(retrieved.getAuditMetadata()).isNotNull();
        }

        @Test
        @DisplayName("should save and retrieve FreeGroup with additional member")
        void shouldSaveAndRetrieveFreeGroupWithAdditionalMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Sprint Team", OWNER));
            addMemberViaInvitation(group, EXTRA_MEMBER);

            UserGroup saved = userGroupRepository.save(group);
            Optional<UserGroup> found = userGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            UserGroup retrieved = found.get();
            assertThat(retrieved.getMembers()).hasSize(2);
            assertThat(retrieved.hasMember(OWNER)).isTrue();
            assertThat(retrieved.hasMember(EXTRA_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return empty when group not found")
        void shouldReturnEmptyWhenGroupNotFound() {
            Optional<UserGroup> found = userGroupRepository.findById(new UserGroupId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should persist audit metadata after save")
        void shouldPersistAuditMetadataAfterSave() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Audit Test Group", OWNER));

            UserGroup saved = userGroupRepository.save(group);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter.byMember())")
    class FindAllByMember {

        @Test
        @DisplayName("should return groups where member is a member")
        void shouldReturnGroupsForMember() {
            FreeGroup group1 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Alpha", OWNER));
            addMemberViaInvitation(group1, EXTRA_MEMBER);
            userGroupRepository.save(group1);

            FreeGroup group2 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Beta", OWNER));
            addMemberViaInvitation(group2, EXTRA_MEMBER);
            userGroupRepository.save(group2);

            FreeGroup group3 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Gamma", OWNER));
            userGroupRepository.save(group3);

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byMember(EXTRA_MEMBER));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserGroup::getName)
                    .containsExactlyInAnyOrder("Group Alpha", "Group Beta");
        }

        @Test
        @DisplayName("should return empty list when member belongs to no groups")
        void shouldReturnEmptyListForMemberWithNoGroups() {
            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byMember(EXTRA_MEMBER));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include group where member is the owner")
        void shouldIncludeGroupWhereOwnerIsSearchedMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Owner's Group", OWNER));
            userGroupRepository.save(group);

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byMember(OWNER));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Owner's Group");
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteMethod {

        @Test
        @DisplayName("should delete group so it can no longer be found")
        void shouldDeleteGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("To Be Deleted", OWNER));
            UserGroup saved = userGroupRepository.save(group);
            UserGroupId id = saved.getId();

            userGroupRepository.delete(id);

            assertThat(userGroupRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter) — filter by type")
    class FindAllByType {

        @Test
        @DisplayName("should return only training groups when filtering by TRAINING type")
        void shouldReturnOnlyTrainingGroups() {
            userGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Juniors", OWNER, new AgeRange(8, 14))));
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Trail Runners", OWNER)));
            userGroupRepository.save(FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Smith Family", OWNER, Set.of())));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byType(GroupType.TRAINING));

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isInstanceOf(TrainingGroup.class);
            assertThat(result.get(0).getName()).isEqualTo("Juniors");
        }

        @Test
        @DisplayName("should return only family groups when filtering by FAMILY type")
        void shouldReturnOnlyFamilyGroups() {
            userGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Seniors", OWNER, new AgeRange(18, 60))));
            userGroupRepository.save(FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Johnson Family", OWNER, Set.of())));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byType(GroupType.FAMILY));

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isInstanceOf(FamilyGroup.class);
            assertThat(result.get(0).getName()).isEqualTo("Johnson Family");
        }

        @Test
        @DisplayName("should return empty list when no groups of given type exist")
        void shouldReturnEmptyWhenNoGroupsOfType() {
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Cyclists", OWNER)));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byType(GroupType.TRAINING));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter) — filter by id")
    class FindAllById {

        @Test
        @DisplayName("should return exactly the group matching the given id")
        void shouldReturnGroupMatchingId() {
            UserGroup saved = userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Specific Group", OWNER)));
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Other Group", OWNER)));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byId(saved.getId()));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("should return empty list when no group matches the given id")
        void shouldReturnEmptyWhenIdNotFound() {
            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byId(new UserGroupId(UUID.randomUUID())));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter) — filter by owner")
    class FindAllByOwner {

        @Test
        @DisplayName("should return groups owned by the given member")
        void shouldReturnGroupsOwnedByMember() {
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Owner's Group", OWNER)));
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Extra's Group", EXTRA_MEMBER)));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byOwner(OWNER));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Owner's Group");
        }

        @Test
        @DisplayName("should return empty list when member owns no groups")
        void shouldReturnEmptyWhenMemberOwnsNoGroups() {
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Owner's Group", OWNER)));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byOwner(EXTRA_MEMBER));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter) — filter by member")
    class FindAllByMemberFilter {

        @Test
        @DisplayName("should return groups where the given member is a member")
        void shouldReturnGroupsWhereMemberBelongs() {
            FreeGroup group1 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Alpha", OWNER));
            addMemberViaInvitation(group1, EXTRA_MEMBER);
            userGroupRepository.save(group1);

            FreeGroup group2 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Beta", OWNER));
            userGroupRepository.save(group2);

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byMember(EXTRA_MEMBER));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Group Alpha");
        }

        @Test
        @DisplayName("should return empty list when member belongs to no groups")
        void shouldReturnEmptyWhenMemberBelongsToNoGroups() {
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Owner's Group", OWNER)));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byMember(EXTRA_MEMBER));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter) — filter by pendingInvitationForMember")
    class FindAllByPendingInvitation {

        @Test
        @DisplayName("should return groups with pending invitation for the given member")
        void shouldReturnGroupsWithPendingInvitation() {
            FreeGroup group1 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group With Invite", OWNER));
            group1.invite(OWNER, EXTRA_MEMBER);
            userGroupRepository.save(group1);

            FreeGroup group2 = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Without Invite", OWNER));
            userGroupRepository.save(group2);

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byPendingInvitation(EXTRA_MEMBER));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Group With Invite");
        }

        @Test
        @DisplayName("should not return group when invitation was accepted")
        void shouldNotReturnGroupWhenInvitationAccepted() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group", OWNER));
            addMemberViaInvitation(group, EXTRA_MEMBER);
            userGroupRepository.save(group);

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byPendingInvitation(EXTRA_MEMBER));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter) — combination of filters")
    class FindAllWithCombinedFilters {

        @Test
        @DisplayName("should return only training groups where member belongs (type + member)")
        void shouldFilterByTypeAndMember() {
            TrainingGroup training = TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Training Group", OWNER, new AgeRange(8, 14)));
            training.assignEligibleMember(EXTRA_MEMBER);
            userGroupRepository.save(training);

            FreeGroup free = FreeGroup.create(new FreeGroup.CreateFreeGroup("Free Group", OWNER));
            addMemberViaInvitation(free, EXTRA_MEMBER);
            userGroupRepository.save(free);

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.byTypeAndMember(GroupType.TRAINING, EXTRA_MEMBER));

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isInstanceOf(TrainingGroup.class);
            assertThat(result.get(0).getName()).isEqualTo("Training Group");
        }
    }

    @Nested
    @DisplayName("findAll(GroupFilter.all()) — no filter returns all groups")
    class FindAllNoFilter {

        @Test
        @DisplayName("should return all groups when no filter is applied")
        void shouldReturnAllGroupsForEmptyFilter() {
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Group A", OWNER)));
            userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Group B", OWNER)));
            userGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Training C", OWNER, new AgeRange(10, 18))));

            List<UserGroup> result = userGroupRepository.findAll(GroupFilter.all());

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findOne(GroupFilter)")
    class FindOneFilter {

        @Test
        @DisplayName("should return Optional with first match when groups exist")
        void shouldReturnFirstMatchingGroup() {
            UserGroup saved = userGroupRepository.save(FreeGroup.create(new FreeGroup.CreateFreeGroup("Match Group", OWNER)));

            Optional<UserGroup> result = userGroupRepository.findOne(GroupFilter.byId(saved.getId()));

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("should return empty Optional when no group matches")
        void shouldReturnEmptyWhenNoMatch() {
            Optional<UserGroup> result = userGroupRepository.findOne(GroupFilter.byId(new UserGroupId(UUID.randomUUID())));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return family group when filtering by type and member (type + member)")
        void shouldReturnFamilyGroupForMember() {
            FamilyGroup family = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Test Family", OWNER, Set.of(EXTRA_MEMBER)));
            userGroupRepository.save(family);

            Optional<UserGroup> result = userGroupRepository.findOne(GroupFilter.byTypeAndMember(GroupType.FAMILY, EXTRA_MEMBER));

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(FamilyGroup.class);
        }
    }

    @Nested
    @DisplayName("Domain event publication — UserGroupMemento @DomainEvents delegation")
    class DomainEventPublication {

        @org.junit.jupiter.api.BeforeEach
        void clearEvents() {
            domainEventCapture.clear();
        }

        @Test
        @DisplayName("should publish MemberAssignedToTrainingGroupEvent when saving TrainingGroup after assignEligibleMember")
        void shouldPublishEventWhenSavingTrainingGroupAfterAssignEligibleMember() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Juniors", OWNER, new AgeRange(10, 18)));
            group.assignEligibleMember(EXTRA_MEMBER);

            userGroupRepository.save(group);

            assertThat(domainEventCapture.getCapturedEvents()).hasSize(1);
            MemberAssignedToTrainingGroupEvent event =
                    (MemberAssignedToTrainingGroupEvent) domainEventCapture.getCapturedEvents().get(0);
            assertThat(event.memberId()).isEqualTo(EXTRA_MEMBER);
            assertThat(event.groupName()).isEqualTo("Juniors");
        }

        @Test
        @DisplayName("should not publish any event when saving TrainingGroup without member changes")
        void shouldNotPublishEventWhenNoMemberChanges() {
            TrainingGroup group = TrainingGroup.create(
                    new TrainingGroup.CreateTrainingGroup("Seniors", OWNER, new AgeRange(18, 40)));

            userGroupRepository.save(group);

            assertThat(domainEventCapture.getCapturedEvents()).isEmpty();
        }
    }

    private static void addMemberViaInvitation(FreeGroup group, MemberId member) {
        group.invite(OWNER, member);
        InvitationId invitationId = group.getPendingInvitations().stream()
                .filter(inv -> inv.getInvitedMember().equals(member))
                .findFirst().orElseThrow().getId();
        group.acceptInvitation(invitationId);
    }
}
