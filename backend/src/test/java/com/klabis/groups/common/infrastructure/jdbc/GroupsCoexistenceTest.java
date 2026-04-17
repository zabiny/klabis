package com.klabis.groups.common.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.members.MemberId;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.common.domain.FamilyGroupFilter;
import com.klabis.groups.common.domain.MembersGroupFilter;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.membersgroup.domain.MembersGroup;
import com.klabis.groups.membersgroup.MembersGroupId;
import com.klabis.groups.membersgroup.domain.MembersGroupRepository;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Groups coexistence test — type discriminator prevents cross-type leaks")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TEST001', 'Owner', 'Member', '1985-01-01', 'CZ', 'MALE', 'owner@example.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'TEST002', 'Shared', 'Member', '2010-01-01', 'CZ', 'MALE', 'shared@example.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class GroupsCoexistenceTest {

    @Autowired
    private MembersGroupRepository membersGroupRepository;

    @Autowired
    private TrainingGroupRepository trainingGroupRepository;

    @Autowired
    private FamilyGroupRepository familyGroupRepository;

    private static final MemberId OWNER = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId SHARED_MEMBER = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Test
    @DisplayName("findAll with ownerOrMemberIs filter returns only MembersGroup (FREE) — not TRAINING or FAMILY rows")
    void membersGroupRepositoryFindsOnlyFreeType() {
        saveMembersGroupWithMember();
        saveTrainingGroupWithMember();
        saveFamilyGroupWithMember();

        List<MembersGroup> result = membersGroupRepository.findAll(
                MembersGroupFilter.all().withOwnerOrMemberIs(SHARED_MEMBER));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Free Group");
    }

    @Test
    @DisplayName("findOne(withMemberIs) returns only TrainingGroup (TRAINING) — not FREE or FAMILY rows")
    void trainingGroupRepositoryFindsOnlyTrainingType() {
        saveMembersGroupWithMember();
        saveTrainingGroupWithMember();
        saveFamilyGroupWithMember();

        var result = trainingGroupRepository.findOne(TrainingGroupFilter.all().withMemberIs(SHARED_MEMBER));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Training Group");
    }

    @Test
    @DisplayName("findOne(withMemberOrParentIs) returns only FamilyGroup (FAMILY) — not FREE or TRAINING rows")
    void familyGroupRepositoryFindsOnlyFamilyType() {
        saveMembersGroupWithMember();
        saveTrainingGroupWithMember();
        saveFamilyGroupWithMember();

        var result = familyGroupRepository.findOne(
                FamilyGroupFilter.all().withMemberOrParentIs(SHARED_MEMBER));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Family Group");
    }

    @Test
    @DisplayName("findById on MembersGroupRepository with a TrainingGroup UUID returns empty")
    void findByIdCrossTypeLookupReturnsEmpty() {
        TrainingGroup training = saveTrainingGroupWithMember();
        MembersGroupId trainingIdAsFreeLookup = new MembersGroupId(training.getId().value());

        var result = membersGroupRepository.findById(trainingIdAsFreeLookup);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("delete on MembersGroupRepository with a TrainingGroup UUID does not remove the TrainingGroup row")
    void deleteCrossTypeIsNoOp() {
        TrainingGroup training = saveTrainingGroupWithMember();
        MembersGroupId trainingIdAsFreeLookup = new MembersGroupId(training.getId().value());

        membersGroupRepository.delete(trainingIdAsFreeLookup);

        assertThat(trainingGroupRepository.findById(training.getId())).isPresent();
    }

    private void saveMembersGroupWithMember() {
        MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Free Group", OWNER));
        group.invite(OWNER, SHARED_MEMBER);
        group.acceptInvitation(group.getPendingInvitations().get(0).getId());
        membersGroupRepository.save(group);
    }

    private TrainingGroup saveTrainingGroupWithMember() {
        TrainingGroup group = TrainingGroup.create(
                new TrainingGroup.CreateTrainingGroup("Training Group", OWNER, new AgeRange(5, 18)));
        group.assignEligibleMember(SHARED_MEMBER);
        group.clearDomainEvents();
        return trainingGroupRepository.save(group);
    }

    private void saveFamilyGroupWithMember() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Family Group", OWNER));
        group.addChild(SHARED_MEMBER);
        familyGroupRepository.save(group);
    }
}
