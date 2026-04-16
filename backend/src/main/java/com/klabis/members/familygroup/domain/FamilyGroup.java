package com.klabis.members.familygroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.MemberAlreadyInGroupException;
import com.klabis.common.usergroup.UserGroup;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

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
    public record CreateFamilyGroup(String name, MemberId parent) {
        public CreateFamilyGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(parent, "Parent is required");
        }
    }

    public static FamilyGroup create(CreateFamilyGroup command) {
        FamilyGroupId id = new FamilyGroupId(UUID.randomUUID());
        UserId ownerId = command.parent().toUserId();
        UserGroup userGroup = UserGroup.create(command.name(), ownerId);
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

    public Set<GroupMembership> getChildren() {
        Set<MemberId> parents = getParents();
        return getMembers().stream()
                .filter(m -> !parents.contains(MemberId.fromUserId(m.userId())))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasMember(MemberId memberId) {
        return userGroup.hasMember(memberId.toUserId());
    }

    public boolean isLastParent(MemberId memberId) {
        return userGroup.isLastOwner(memberId.toUserId());
    }

    public void addParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        userGroup.addOwner(parent.toUserId());
        if (!userGroup.hasMember(parent.toUserId())) {
            userGroup.addMember(parent.toUserId());
        }
    }

    public void removeParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        userGroup.removeOwner(parent.toUserId());
        userGroup.removeMember(parent.toUserId());
    }

    public void addChild(MemberId child) {
        Assert.notNull(child, "Child MemberId is required");
        if (userGroup.isOwner(child.toUserId())) {
            throw new MemberAlreadyInGroupException(child.toUserId());
        }
        userGroup.addMember(child.toUserId());
    }

    public void removeChild(MemberId child) {
        Assert.notNull(child, "Child MemberId is required");
        userGroup.removeMember(child.toUserId());
    }
}
