package com.klabis.members.familygroup.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.common.domain.FamilyGroupFilter;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FamilyGroup JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'TEST001', 'Parent', 'One', '1985-01-01', 'CZ', 'FEMALE', 'parent1@example.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'TEST002', 'Parent', 'Two', '1983-06-15', 'CZ', 'MALE', 'parent2@example.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TEST003', 'Child', 'One', '2012-03-20', 'CZ', 'FEMALE', 'child1@example.com', '+420333333333', 'Street 3', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class FamilyGroupPersistenceTest {

    @Autowired
    private FamilyGroupRepository familyGroupRepository;

    private static final MemberId PARENT_A = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId PARENT_B = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId CHILD_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Nested
    @DisplayName("save() and findById() — round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should save and retrieve family group with parent")
        void shouldSaveAndRetrieveFamilyGroup() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            FamilyGroup saved = familyGroupRepository.save(group);
            Optional<FamilyGroup> found = familyGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            FamilyGroup retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Novákovi");
            assertThat(retrieved.getParents()).containsExactly(PARENT_A);
            assertThat(retrieved.hasMember(PARENT_A)).isTrue();
            assertThat(retrieved.getAuditMetadata()).isNotNull();
        }

        @Test
        @DisplayName("should save and retrieve family group with a child member added after creation")
        void shouldSaveAndRetrieveFamilyGroupWithChild() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addChild(CHILD_A);

            FamilyGroup saved = familyGroupRepository.save(group);
            Optional<FamilyGroup> found = familyGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().hasMember(CHILD_A)).isTrue();
        }

        @Test
        @DisplayName("should return empty when group not found")
        void shouldReturnEmptyWhenGroupNotFound() {
            Optional<FamilyGroup> found = familyGroupRepository.findById(new FamilyGroupId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should persist audit metadata after save")
        void shouldPersistAuditMetadataAfterSave() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Audit Test", PARENT_A));

            FamilyGroup saved = familyGroupRepository.save(group);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findAll(FamilyGroupFilter)")
    class FindAll {

        @Test
        @DisplayName("should return all saved family groups when filter is all()")
        void shouldReturnAllGroups() {
            familyGroupRepository.save(FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A)));
            familyGroupRepository.save(FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Svobodovi", PARENT_B)));

            List<FamilyGroup> result = familyGroupRepository.findAll(FamilyGroupFilter.all());

            assertThat(result).hasSize(2);
            assertThat(result).extracting(FamilyGroup::getName)
                    .containsExactlyInAnyOrder("Novákovi", "Svobodovi");
        }

        @Test
        @DisplayName("should return empty list when no groups exist")
        void shouldReturnEmptyListWhenNoGroups() {
            List<FamilyGroup> result = familyGroupRepository.findAll(FamilyGroupFilter.all());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOne(FamilyGroupFilter.withMemberOrParentIs)")
    class FindOneWithMemberOrParent {

        @Test
        @DisplayName("should find group by parent")
        void shouldFindGroupByParent() {
            familyGroupRepository.save(FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A)));

            Optional<FamilyGroup> found = familyGroupRepository.findOne(
                    FamilyGroupFilter.all().withMemberOrParentIs(PARENT_A));

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Novákovi");
        }

        @Test
        @DisplayName("should find group by child member")
        void shouldFindGroupByChildMember() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addChild(CHILD_A);
            familyGroupRepository.save(group);

            Optional<FamilyGroup> found = familyGroupRepository.findOne(
                    FamilyGroupFilter.all().withMemberOrParentIs(CHILD_A));

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Novákovi");
        }

        @Test
        @DisplayName("should return empty when member is not in any group")
        void shouldReturnEmptyWhenMemberNotInAnyGroup() {
            Optional<FamilyGroup> found = familyGroupRepository.findOne(
                    FamilyGroupFilter.all().withMemberOrParentIs(PARENT_A));

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteMethod {

        @Test
        @DisplayName("should delete group so it can no longer be found")
        void shouldDeleteGroup() {
            FamilyGroup group = familyGroupRepository.save(
                    FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("To Be Deleted", PARENT_A)));
            FamilyGroupId id = group.getId();

            familyGroupRepository.delete(id);

            assertThat(familyGroupRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("update — parent management")
    class UpdateParents {

        @Test
        @DisplayName("should persist added parent after save")
        void shouldPersistAddedParent() {
            FamilyGroup group = familyGroupRepository.save(
                    FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A)));

            group.addParent(PARENT_B);
            familyGroupRepository.save(group);

            FamilyGroup retrieved = familyGroupRepository.findById(group.getId()).orElseThrow();
            assertThat(retrieved.getParents()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(retrieved.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should persist removed parent — removed from both parents and members tables")
        void shouldPersistRemovedParent() {
            FamilyGroup group = familyGroupRepository.save(
                    FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A)));
            group.addParent(PARENT_B);
            group = familyGroupRepository.save(group);

            group.removeParent(PARENT_B);
            familyGroupRepository.save(group);

            FamilyGroup retrieved = familyGroupRepository.findById(group.getId()).orElseThrow();
            assertThat(retrieved.getParents()).containsExactly(PARENT_A);
            assertThat(retrieved.hasMember(PARENT_B)).isFalse();
        }
    }

    @Nested
    @DisplayName("findOne(FamilyGroupFilter) — deduplication")
    class FindOneDeduplication {

        @Test
        @DisplayName("should return single result when member is also a parent (present in both tables)")
        void shouldReturnSingleResultWhenParentIsAlsoInChildrenTable() {
            // PARENT_A is stored in both user_group_owners and user_group_members
            familyGroupRepository.save(FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A)));

            Optional<FamilyGroup> found = familyGroupRepository.findOne(
                    FamilyGroupFilter.all().withMemberOrParentIs(PARENT_A));

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Novákovi");
        }
    }

    @Nested
    @DisplayName("findOne(FamilyGroupFilter) — contract")
    class FindOneContract {

        @Test
        @DisplayName("should throw IllegalStateException when filter matches more than one group (invariant violation)")
        void shouldThrowWhenMemberAppearsInTwoFamilyGroups() {
            familyGroupRepository.save(
                    FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Family One", PARENT_A)));
            FamilyGroup group2 = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Family Two", PARENT_B));
            group2.addChild(PARENT_A);
            familyGroupRepository.save(group2);

            assertThatThrownBy(() ->
                    familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(PARENT_A)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("findOne expected at most 1 result");
        }
    }
}
