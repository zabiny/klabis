package com.klabis.members.familygroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.UserGroup;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AggregateRoot
public class FamilyGroup extends KlabisAggregateRoot<FamilyGroup, FamilyGroupId> {

    public static final String TYPE_DISCRIMINATOR = "FAMILY";

    @Identity
    private final FamilyGroupId id;
    private final UserGroup userGroup;

    private FamilyGroup(FamilyGroupId id, UserGroup userGroup) {
        Assert.notNull(id, "FamilyGroupId is required");
        Assert.notNull(userGroup, "UserGroup is required");
        this.id = id;
        this.userGroup = userGroup;
    }

    // Parents are the semantic concept for owners in a family group context.
    // parent = owner + member: adding a parent grants ownership and membership,
    // removing a parent withdraws both ownership and membership entirely.
    @RecordBuilder
    public record CreateFamilyGroup(String name, Set<MemberId> parents, Set<MemberId> initialMembers) {
        public CreateFamilyGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(parents, "Parents set is required");
            Assert.notEmpty(parents, "At least one parent is required");
            Assert.notNull(initialMembers, "Initial members set is required");
        }
    }

    public static FamilyGroup create(CreateFamilyGroup command) {
        FamilyGroupId id = new FamilyGroupId(UUID.randomUUID());
        Set<UserId> ownerIds = command.parents().stream()
                .map(MemberId::toUserId)
                .collect(Collectors.toSet());
        // Parents are owners AND members; initial members are members only
        Set<GroupMembership> memberships = new HashSet<>();
        command.parents().forEach(p -> memberships.add(GroupMembership.of(p.toUserId())));
        command.initialMembers().forEach(m -> memberships.add(GroupMembership.of(m.toUserId())));
        UserGroup userGroup = UserGroup.reconstruct(command.name(), ownerIds, memberships);
        return new FamilyGroup(id, userGroup);
    }

    public static FamilyGroup reconstruct(FamilyGroupId id, String name, Set<MemberId> parents,
                                          Set<GroupMembership> members, AuditMetadata auditMetadata) {
        Set<UserId> ownerIds = parents.stream()
                .map(MemberId::toUserId)
                .collect(Collectors.toSet());
        UserGroup userGroup = UserGroup.reconstruct(name, ownerIds, members);
        FamilyGroup group = new FamilyGroup(id, userGroup);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public FamilyGroupId getId() {
        return id;
    }

    public String getName() {
        return userGroup.getName();
    }

    public Set<MemberId> getParents() {
        return userGroup.getOwners().stream()
                .map(MemberId::fromUserId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<GroupMembership> getMembers() {
        return userGroup.getMembers();
    }

    public boolean hasMember(MemberId memberId) {
        return userGroup.hasMember(memberId.toUserId());
    }

    public void addParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        userGroup.addMember(parent.toUserId());
        userGroup.addOwner(parent.toUserId());
    }

    public void removeParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        userGroup.removeOwner(parent.toUserId());
        userGroup.removeMember(parent.toUserId());
    }
}
