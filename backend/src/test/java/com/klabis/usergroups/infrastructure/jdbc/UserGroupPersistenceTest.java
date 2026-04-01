package com.klabis.usergroups.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserGroup JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'TEST001', 'Owner', 'Member', '2000-01-01', 'CZ', 'MALE', 'owner@example.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'TEST002', 'Extra', 'Member', '2000-01-01', 'CZ', 'MALE', 'extra@example.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class UserGroupPersistenceTest {

    @Autowired
    private UserGroupRepository userGroupRepository;

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
    @DisplayName("findAllByMember()")
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

            List<UserGroup> result = userGroupRepository.findAllByMember(EXTRA_MEMBER);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserGroup::getName)
                    .containsExactlyInAnyOrder("Group Alpha", "Group Beta");
        }

        @Test
        @DisplayName("should return empty list when member belongs to no groups")
        void shouldReturnEmptyListForMemberWithNoGroups() {
            List<UserGroup> result = userGroupRepository.findAllByMember(EXTRA_MEMBER);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include group where member is the owner")
        void shouldIncludeGroupWhereOwnerIsSearchedMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Owner's Group", OWNER));
            userGroupRepository.save(group);

            List<UserGroup> result = userGroupRepository.findAllByMember(OWNER);

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

    private static void addMemberViaInvitation(FreeGroup group, MemberId member) {
        group.invite(OWNER, member);
        InvitationId invitationId = group.getPendingInvitations().stream()
                .filter(inv -> inv.getInvitedMember().equals(member))
                .findFirst().orElseThrow().getId();
        group.acceptInvitation(invitationId);
    }
}
