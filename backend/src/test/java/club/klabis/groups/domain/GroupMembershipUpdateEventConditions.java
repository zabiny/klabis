package club.klabis.groups.domain;

import club.klabis.members.MemberId;
import club.klabis.shared.config.security.ApplicationGrant;
import org.assertj.core.api.Condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.allOf;

class GroupMembershipUpdateEventConditions {

    static Condition<GroupMembershipUpdated> groupId(int expectedGroupIdValue) {
        return groupId(new MemberGroup.Id(expectedGroupIdValue));
    }

    static Condition<GroupMembershipUpdated> groupId(MemberGroup.Id expectedId) {
        return new Condition<>((GroupMembershipUpdated item) -> expectedId.equals(item.getGroupId()),
                "has groupId=%d", expectedId.value());
    }

    static Condition<GroupMembershipUpdated> memberId(int expectedMemberIdValue) {
        return memberId(new MemberId(expectedMemberIdValue));
    }


    static Condition<GroupMembershipUpdated> memberId(MemberId expectedMemberId) {
        return new Condition<>((GroupMembershipUpdated item) -> expectedMemberId.equals(item.getMemberId()),
                "has memberId=%s", expectedMemberId.value());
    }

    static Condition<GroupMembershipUpdated> grant(ApplicationGrant expectedGrant) {
        return new Condition<>((GroupMembershipUpdated item) -> item.getGrants().contains(expectedGrant),
                "has grant %s", expectedGrant.name());
    }

    static Condition<GroupMembershipUpdated> groupMembershipUpdateEvent(MemberGroup.Id expectedGroupId, MemberId expectedMemberId, ApplicationGrant... expectedGrants) {
        Collection<Condition<GroupMembershipUpdated>> conditions = new ArrayList<>();
        conditions.add(groupId(expectedGroupId));
        conditions.add(memberId(expectedMemberId));
        Stream.of(expectedGrants)
                .map(GroupMembershipUpdateEventConditions::grant)
                .forEach(conditions::add);
        return allOf(conditions);
    }
}
