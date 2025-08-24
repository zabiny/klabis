package club.klabis.groups.domain;

import club.klabis.groups.domain.forms.EditGroup;
import club.klabis.members.MemberId;
import club.klabis.shared.ApplicationGrant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AggregatedRootTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static club.klabis.groups.domain.GroupMembershipUpdateEventConditions.groupMembershipUpdateEvent;
import static club.klabis.groups.domain.MemberGroupConditions.*;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.AggregatedRootTestUtils.assertThatDomainEventsOf;

class MemberGroupTest {

    private static final MemberId GROUP_ADMIN = new MemberId(1);

    private static final MemberId GROUP_MEMBER = new MemberId(5);

    private static EditGroup createEditGroupRequest() {
        return new EditGroup("Example",
                "Description of the example group",
                "exampleGroup@zabiny.club",
                List.of(MemberGroup.GroupPermission.forOwnerOnly(ApplicationGrant.MEMBERS_REGISTER)),
                List.of(GROUP_MEMBER));
    }

    // creates group named Example and email exampleGroup@zabiny.club owned by member with ID 1 and single member with ID 5 granting MEMBERS_REGISTER permission for admins/owners
    private static MemberGroup createMemberGroup() {
        EditGroup createRequest = new EditGroup("Example",
                "Description of the example group",
                "exampleGroup@zabiny.club",
                List.of(MemberGroup.GroupPermission.forAll(ApplicationGrant.MEMBERS_REGISTER)),
                List.of(GROUP_MEMBER));

        MemberGroup group = MemberGroup.createGroup(createRequest, GROUP_ADMIN);
        AggregatedRootTestUtils.clearDomainEvents(group);
        return group;
    }

    private static EditGroup createEditGroupRequestFor(MemberGroup group) {
        return new EditGroup(group.getName(),
                group.getDescription(),
                group.getEmail(),
                group.getPermissions(),
                group.getMembers());
    }

    @DisplayName("createGroup tests")
    @Nested
    class CreateGroupTests {

        @DisplayName("it should create group with correct data")
        @Test
        void itShouldCreateGroupWithCorrectData() {
            EditGroup request = new EditGroup("Example", "Description of the group", "group@zabiny.club", List.of(
                    MemberGroup.GroupPermission.forAll(ApplicationGrant.MEMBERS_EDIT)
            ), Stream.of(3, 4, 5).map(MemberId::new).toList());

            MemberGroup createdGroup = MemberGroup.createGroup(request, new MemberId(1));

            assertThat(createdGroup)
                    .has(allOf(
                            owner(GROUP_ADMIN),
                            hasMembersCount(3),
                            containsMember(new MemberId(3)),
                            containsMember(new MemberId(4)),
                            containsMember(new MemberId(5)),
                            hasName("Example"),
                            hasDescription("Description of the group"),
                            hasGroupEmail("group@zabiny.club")
                    ));
        }

        @DisplayName("it should prepare GroupMembershipUpdated events for owner and all members for newly created MemberGroup if group contains some permissions")
        @Test
        void itShouldPrepareExpectedEvents() {
            EditGroup request = new EditGroup("Test", "something", "email@zabiny.club")
                    .withMembers(Stream.of(4, 5).map(MemberId::new).toList())
                    .withPermissions(
                            MemberGroup.GroupPermission.forAll(ApplicationGrant.MEMBERS_EDIT),
                            MemberGroup.GroupPermission.forOwnerOnly(ApplicationGrant.MEMBERS_REGISTER),
                            MemberGroup.GroupPermission.forMembersOnly(ApplicationGrant.APPUSERS_PERMISSIONS)
                    );

            MemberGroup createdGroup = MemberGroup.createGroup(request, new MemberId(1));

            assertThatDomainEventsOf(createdGroup, GroupMembershipUpdated.class)
                    .haveExactly(1,
                            // administrator has both member and admin permissions
                            groupMembershipUpdateEvent(createdGroup.getId(),
                                    createdGroup.getAdministrator(),
                                    ApplicationGrant.MEMBERS_EDIT,
                                    ApplicationGrant.MEMBERS_REGISTER,
                                    ApplicationGrant.APPUSERS_PERMISSIONS))
                    .haveExactly(1,
                            groupMembershipUpdateEvent(createdGroup.getId(),
                                    new MemberId(4),
                                    ApplicationGrant.MEMBERS_EDIT,
                                    ApplicationGrant.APPUSERS_PERMISSIONS))
                    .haveExactly(1,
                            groupMembershipUpdateEvent(createdGroup.getId(),
                                    new MemberId(5),
                                    ApplicationGrant.MEMBERS_EDIT,
                                    ApplicationGrant.APPUSERS_PERMISSIONS))
                    .hasSize(3);
        }

        @DisplayName("it should not prepare any GroupMembershipUpdated events if created group doesn't have permissions")
        @Test
        void itShouldNotPrepareExpectedEventsForGroupWithoutPermissions() {
            EditGroup request = new EditGroup("name", "description", "email")
                    .withMembers(Stream.of(4, 5).map(MemberId::new).toList())
                    .withPermissions();

            MemberGroup createdGroup = MemberGroup.createGroup(request, new MemberId(1));

            assertThatDomainEventsOf(createdGroup)
                    .isEmpty();
        }

    }


    @DisplayName("update tests")
    @Nested
    class UpdateTests {

        @DisplayName("it should change group name")
        @Test
        void itShouldChangeGroupName() {
            MemberGroup group = createMemberGroup();

            EditGroup editRequest = createEditGroupRequestFor(group)
                    .withName("Updated name");

            group.update(editRequest);

            assertThat(group).has(hasName("Updated name"));
            assertThatDomainEventsOf(group).isEmpty();
        }

        @DisplayName("it should change group email")
        @Test
        void itShouldChangeGroupEmail() {
            MemberGroup group = createMemberGroup();

            EditGroup editRequest = createEditGroupRequestFor(group)
                    .withEmail("changedemail@test.com");

            group.update(editRequest);

            assertThat(group).has(hasGroupEmail("changedemail@test.com"));
            assertThatDomainEventsOf(group).isEmpty();
        }

        @DisplayName("it should change group description")
        @Test
        void itShouldChangeGroupDescription() {
            MemberGroup group = createMemberGroup();

            EditGroup editRequest = createEditGroupRequestFor(group)
                    .withDescription("Updated description");

            group.update(editRequest);

            assertThat(group).has(hasDescription("Updated description"));
            assertThatDomainEventsOf(group).isEmpty();
        }

        @DisplayName("it should change group members")
        @Test
        void itShouldChangeGroupMembers() {
            MemberGroup group = createMemberGroup();

            final MemberId addedMember = new MemberId(7);
            EditGroup editRequest = createEditGroupRequestFor(group)
                    .withMembers(List.of(addedMember));

            group.update(editRequest);

            assertThat(group).has(allOf(hasMembersCount(1), containsMember(addedMember)));
            assertThatDomainEventsOf(group, GroupMembershipUpdated.class)
                    // event for removed member
                    .haveExactly(1, groupMembershipUpdateEvent(group.getId(), GROUP_MEMBER))
                    // event for added member
                    .haveExactly(1,
                            groupMembershipUpdateEvent(group.getId(), addedMember, ApplicationGrant.MEMBERS_REGISTER));
        }

        @DisplayName("it should change group permissions")
        @Test
        void itShouldChangeGroupPermissions() {
            MemberGroup group = createMemberGroup();

            EditGroup editRequest = createEditGroupRequestFor(group)
                    .withPermissions(MemberGroup.GroupPermission.forAll(ApplicationGrant.APPUSERS_PERMISSIONS));

            group.update(editRequest);

            assertThat(group).has(hasPermissions(MemberGroup.GroupPermission.forAll(ApplicationGrant.APPUSERS_PERMISSIONS)));
            assertThatDomainEventsOf(group, GroupMembershipUpdated.class)
                    .haveExactly(1,
                            groupMembershipUpdateEvent(group.getId(),
                                    GROUP_MEMBER,
                                    ApplicationGrant.APPUSERS_PERMISSIONS))
                    .haveExactly(1,
                            groupMembershipUpdateEvent(group.getId(),
                                    GROUP_ADMIN,
                                    ApplicationGrant.APPUSERS_PERMISSIONS));
        }
    }

    @DisplayName("transferOwnership tests")
    @Nested
    class TransferOwnershipTests {
        @DisplayName("it should change group administrator and prepare correct events")
        @Test
        void itShouldPublishEventsForNewOwner() {
            MemberGroup group = createMemberGroup();

            final var newAdministratorId = new MemberId(2);

            group.transferOwnership(newAdministratorId);

            assertThat(group.getAdministrator()).isEqualTo(newAdministratorId);
            assertThatDomainEventsOf(group, GroupMembershipUpdated.class)
                    // new owner message with list of grants from group (= all grants were added for member)
                    .haveExactly(1,
                            groupMembershipUpdateEvent(group.getId(),
                                    newAdministratorId,
                                    ApplicationGrant.MEMBERS_REGISTER))
                    // old owner message with empty list of grants (= all grants were removed)
                    .haveExactly(1, groupMembershipUpdateEvent(group.getId(), GROUP_ADMIN))
                    .hasSize(2);
        }
    }
}

