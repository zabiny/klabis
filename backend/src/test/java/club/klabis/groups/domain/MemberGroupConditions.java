package club.klabis.groups.domain;

import club.klabis.members.MemberId;
import org.assertj.core.api.Condition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class MemberGroupConditions {
    static Condition<MemberGroup> id(int expectedOwnerIdValue) {
        return id(new MemberGroup.Id(expectedOwnerIdValue));
    }

    static Condition<MemberGroup> id(MemberGroup.Id expectedId) {
        return new Condition<>((MemberGroup item) -> item.getId().equals(expectedId), "has id %s", expectedId.value());
    }

    static Condition<MemberGroup> hasName(String expectedName) {
        return new Condition<>((MemberGroup item) -> expectedName.equals(item.getName()), "has name %s", expectedName);
    }


    static Condition<MemberGroup> hasDescription(String expectedDescription) {
        return new Condition<>((MemberGroup item) -> expectedDescription.equals(item.getDescription()), "has description %s", expectedDescription);
    }

    static Condition<MemberGroup> hasGroupEmail(String expectedEmail) {
        return new Condition<>((MemberGroup item) -> expectedEmail.equals(item.getEmail()),
                "has email %s",
                expectedEmail);
    }

    static Condition<MemberGroup> hasPermissions(MemberGroup.GroupPermission... permissions) {
        return new Condition<>((MemberGroup item) -> item.getPermissions().size() == permissions.length && item.getPermissions().containsAll(List.of(permissions)), "has permissions %s",
                Arrays.stream(permissions).map(Objects::toString).collect(Collectors.joining(", ")));
    }

    static Condition<MemberGroup> containsPermission(MemberGroup.GroupPermission expectedPermission) {
        return new Condition<>((MemberGroup item) -> item.getPermissions().stream().anyMatch(expectedPermission::equals),
                "has permission %s",
                expectedPermission.toString());
    }

    static Condition<MemberGroup> owner(int expectedOwnerIdValue) {
        return owner(new MemberId(expectedOwnerIdValue));
    }

    static Condition<MemberGroup> owner(MemberId expectedOwnerId) {
        return new Condition<>((MemberGroup item) -> item.getAdministrator().equals(expectedOwnerId),
                "has owner %s",
                expectedOwnerId.value());
    }

    static Condition<MemberGroup> containsMember(MemberId expectedMemberId) {
        return new Condition<>((MemberGroup item) -> item.getMembers().stream().anyMatch(expectedMemberId::equals),
                "contains member with ID %s",
                expectedMemberId.value());
    }

    static Condition<MemberGroup> hasMembersCount(int expectedMembersCount) {
        return new Condition<>((MemberGroup item) -> item.getMembers().size() == expectedMembersCount,
                "has %d members",
                expectedMembersCount);
    }
}
