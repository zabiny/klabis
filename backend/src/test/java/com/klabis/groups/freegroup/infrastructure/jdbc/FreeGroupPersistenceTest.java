package com.klabis.groups.freegroup.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.members.MemberId;
import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;

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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FreeGroup JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'TEST001', 'Owner', 'One', '1985-01-01', 'CZ', 'FEMALE', 'owner1@example.com', '+420111111111', 'Street 1', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'TEST002', 'Member', 'Two', '1990-06-15', 'CZ', 'MALE', 'member2@example.com', '+420222222222', 'Street 2', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('33333333-3333-3333-3333-333333333333', 'TEST003', 'Invited', 'Three', '1995-03-20', 'CZ', 'MALE', 'invited3@example.com', '+420333333333', 'Street 3', 'City', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class FreeGroupPersistenceTest {

    @Autowired
    private FreeGroupRepository freeGroupRepository;

    private static final MemberId CREATOR = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId INVITED_MEMBER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Nested
    @DisplayName("save() and findById() — round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should save and retrieve basic group")
        void shouldSaveAndRetrieveGroup() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Orienteering Friends", CREATOR));

            FreeGroup saved = freeGroupRepository.save(group);
            Optional<FreeGroup> found = freeGroupRepository.findById(saved.getId());

            assertThat(found).isPresent();
            FreeGroup retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Orienteering Friends");
            assertThat(retrieved.getOwners()).containsExactly(CREATOR);
            assertThat(retrieved.hasMember(CREATOR)).isTrue();
            assertThat(retrieved.isOwner(CREATOR)).isTrue();
            assertThat(retrieved.getAuditMetadata()).isNotNull();
        }

        @Test
        @DisplayName("should return empty when group not found")
        void shouldReturnEmptyWhenGroupNotFound() {
            Optional<FreeGroup> found = freeGroupRepository.findById(new FreeGroupId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should persist audit metadata after save")
        void shouldPersistAuditMetadataAfterSave() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Audit Test", CREATOR));

            FreeGroup saved = freeGroupRepository.save(group);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("save() with invitations — round-trip")
    class InvitationRoundTrip {

        @Test
        @DisplayName("should persist and retrieve pending invitation")
        void shouldPersistAndRetrievePendingInvitation() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            freeGroupRepository.save(group);
            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();

            assertThat(retrieved.getPendingInvitations()).hasSize(1);
            assertThat(retrieved.isInvitedMember(invitationId, INVITED_MEMBER)).isTrue();
            Invitation invitation = retrieved.getPendingInvitations().get(0);
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
            assertThat(invitation.getInvitedUser()).isEqualTo(INVITED_MEMBER.toUserId());
            assertThat(invitation.getInvitedBy()).isEqualTo(CREATOR.toUserId());
        }

        @Test
        @DisplayName("should persist accepted invitation and new member")
        void shouldPersistAcceptedInvitationAndNewMember() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.acceptInvitation(invitationId);
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();

            assertThat(retrieved.hasMember(INVITED_MEMBER)).isTrue();
            assertThat(retrieved.getPendingInvitations()).isEmpty();
            Invitation accepted = retrieved.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should persist rejected invitation without adding member")
        void shouldPersistRejectedInvitationWithoutAddingMember() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.rejectInvitation(invitationId);
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();

            assertThat(retrieved.hasMember(INVITED_MEMBER)).isFalse();
            assertThat(retrieved.getPendingInvitations()).isEmpty();
            Invitation rejected = retrieved.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(rejected.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        }

        @Test
        @DisplayName("should persist and restore cancelled invitation with all audit fields")
        void shouldPersistAndRestoreCancelledInvitation() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.cancelInvitation(invitationId, Optional.of(CREATOR), "No longer needed");
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();

            assertThat(retrieved.hasMember(INVITED_MEMBER)).isFalse();
            assertThat(retrieved.getPendingInvitations()).isEmpty();
            Invitation cancelled = retrieved.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(cancelled.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
            assertThat(cancelled.getCancelledAt()).isPresent();
            assertThat(cancelled.getCancelledBy()).contains(CREATOR);
            assertThat(cancelled.getCancellationReason()).contains("No longer needed");
        }

        @Test
        @DisplayName("should persist cancelled invitation with SYSTEM actor — cancelledBy is empty")
        void shouldPersistCancelledInvitationWithSystemActor() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.cancelInvitation(invitationId, Optional.empty(), "Member deactivated");
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();

            Invitation cancelled = retrieved.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(cancelled.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
            assertThat(cancelled.getCancelledBy()).isEmpty();
            assertThat(cancelled.getCancellationReason()).contains("Member deactivated");
        }
    }

    @Nested
    @DisplayName("update — owner and member management")
    class UpdateOwnerAndMember {

        @Test
        @DisplayName("should persist added owner after save")
        void shouldPersistAddedOwner() {
            FreeGroup group = freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR)));
            group.invite(CREATOR, MEMBER_A);
            group.acceptInvitation(group.getPendingInvitations().get(0).getId());
            group.addOwner(MEMBER_A, CREATOR);
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();
            assertThat(retrieved.isOwner(MEMBER_A)).isTrue();
        }

        @Test
        @DisplayName("should persist removed owner after save")
        void shouldPersistRemovedOwner() {
            FreeGroup group = FreeGroup.reconstruct(
                    new FreeGroupId(UUID.randomUUID()), "Test Group",
                    Set.of(CREATOR, MEMBER_A),
                    Set.of(GroupMembership.of(CREATOR.toUserId()), GroupMembership.of(MEMBER_A.toUserId())),
                    Set.of(), null);
            group = freeGroupRepository.save(group);
            group.removeOwner(MEMBER_A, CREATOR);
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();
            assertThat(retrieved.isOwner(MEMBER_A)).isFalse();
            assertThat(retrieved.hasMember(MEMBER_A)).isTrue();
        }

        @Test
        @DisplayName("should persist removed member after save")
        void shouldPersistRemovedMember() {
            FreeGroup group = freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR)));
            group.invite(CREATOR, MEMBER_A);
            group.acceptInvitation(group.getPendingInvitations().get(0).getId());
            group = freeGroupRepository.save(group);
            group.removeMember(MEMBER_A, CREATOR);
            freeGroupRepository.save(group);

            FreeGroup retrieved = freeGroupRepository.findById(group.getId()).orElseThrow();
            assertThat(retrieved.hasMember(MEMBER_A)).isFalse();
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteMethod {

        @Test
        @DisplayName("should delete group so it can no longer be found")
        void shouldDeleteGroup() {
            FreeGroup group = freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("To Be Deleted", CREATOR)));
            FreeGroupId id = group.getId();

            freeGroupRepository.delete(id);

            assertThat(freeGroupRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(FreeGroupFilter) — owner or member filter")
    class FindAllWithOwnerOrMemberFilter {

        @Test
        @DisplayName("should find groups where member is owner")
        void shouldFindGroupsWhereOwner() {
            freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("Creator's Group", CREATOR)));

            List<FreeGroup> result = freeGroupRepository.findAll(
                    FreeGroupFilter.all().withOwnerOrMemberIs(CREATOR));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Creator's Group");
        }

        @Test
        @DisplayName("should find groups where member joined via invitation")
        void shouldFindGroupsWhereMemberJoinedViaInvitation() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, MEMBER_A);
            group.acceptInvitation(group.getPendingInvitations().get(0).getId());
            freeGroupRepository.save(group);

            List<FreeGroup> result = freeGroupRepository.findAll(
                    FreeGroupFilter.all().withOwnerOrMemberIs(MEMBER_A));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Test Group");
        }

        @Test
        @DisplayName("should return empty list when member is not in any group")
        void shouldReturnEmptyWhenMemberNotInAnyGroup() {
            List<FreeGroup> result = freeGroupRepository.findAll(
                    FreeGroupFilter.all().withOwnerOrMemberIs(INVITED_MEMBER));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(FreeGroupFilter) — pending invitation filter")
    class FindAllWithPendingInvitationFilter {

        @Test
        @DisplayName("should find groups with pending invitations for member")
        void shouldFindGroupsWithPendingInvitationsForMember() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            freeGroupRepository.save(group);

            List<FreeGroup> result = freeGroupRepository.findAll(
                    FreeGroupFilter.all().withPendingInvitationFor(INVITED_MEMBER));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Test Group");
        }

        @Test
        @DisplayName("should not return group when invitation was accepted")
        void shouldNotReturnGroupWhenInvitationAccepted() {
            FreeGroup group = FreeGroup.create(
                    new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, INVITED_MEMBER);
            group.acceptInvitation(group.getPendingInvitations().get(0).getId());
            freeGroupRepository.save(group);

            List<FreeGroup> result = freeGroupRepository.findAll(
                    FreeGroupFilter.all().withPendingInvitationFor(INVITED_MEMBER));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no pending invitations for member")
        void shouldReturnEmptyWhenNoPendingInvitations() {
            List<FreeGroup> result = freeGroupRepository.findAll(
                    FreeGroupFilter.all().withPendingInvitationFor(MEMBER_A));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOne(FreeGroupFilter) — contract")
    class FindOneContract {

        @Test
        @DisplayName("should return empty when no groups match filter")
        void shouldReturnEmptyWhenNoMatch() {
            Optional<FreeGroup> result = freeGroupRepository.findOne(
                    FreeGroupFilter.all().withOwnerOrMemberIs(INVITED_MEMBER));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return single group when exactly one matches filter")
        void shouldReturnGroupWhenExactlyOneMatch() {
            freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("Solo Group", CREATOR)));

            Optional<FreeGroup> result = freeGroupRepository.findOne(
                    FreeGroupFilter.all().withOwnerOrMemberIs(CREATOR));

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Solo Group");
        }

        @Test
        @DisplayName("should throw IllegalStateException when filter matches more than one group")
        void shouldThrowWhenFilterMatchesMultipleGroups() {
            freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("Group One", CREATOR)));
            freeGroupRepository.save(
                    FreeGroup.create(new FreeGroup.CreateFreeGroup("Group Two", CREATOR)));

            assertThatThrownBy(() -> freeGroupRepository.findOne(
                    FreeGroupFilter.all().withOwnerOrMemberIs(CREATOR)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
