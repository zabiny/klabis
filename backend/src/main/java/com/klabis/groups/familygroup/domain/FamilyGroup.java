package com.klabis.groups.familygroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.users.UserId;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.common.domain.MemberAlreadyInGroupException;
import com.klabis.groups.common.domain.MemberGroup;
import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AggregateRoot
public class FamilyGroup extends MemberGroup<FamilyGroup, FamilyGroupId, UserId> {

    public static final String TYPE_DISCRIMINATOR = "FAMILY";

    @Identity
    private final FamilyGroupId id;

    private FamilyGroup(FamilyGroupId id, String name, Set<UserId> parents, Set<GroupMembership<UserId>> members) {
        super(name, parents, members);
        Assert.notNull(id, "FamilyGroupId is required");
        this.id = id;
    }

    // Parents are the semantic concept for owners in a family group context.
    // parent = owner + member: adding a parent grants ownership and membership,
    // removing a parent withdraws both ownership and membership entirely.
    // A parent is a user of the system and need not have a member profile,
    // hence UserId internally; children are always club members, hence MemberId on the public API.
    @RecordBuilder
    public record CreateFamilyGroup(String name, UserId parent) {
        public CreateFamilyGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(parent, "Parent is required");
        }
    }

    public static FamilyGroup create(CreateFamilyGroup command) {
        FamilyGroupId id = new FamilyGroupId(UUID.randomUUID());
        // Parent is both owner and member from the start
        return new FamilyGroup(id, command.name(), Set.of(command.parent()),
                Set.of(GroupMembership.of(command.parent())));
    }

    public static FamilyGroup reconstruct(FamilyGroupId id, String name, Set<UserId> parents,
                                          Set<GroupMembership<UserId>> members, AuditMetadata auditMetadata) {
        FamilyGroup group = new FamilyGroup(id, name, parents, members);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public FamilyGroupId getId() {
        return id;
    }

    public Set<UserId> getParents() {
        return getOwners();
    }

    public Set<GroupMembership<MemberId>> getChildren() {
        Set<UserId> parents = getParents();
        return getMembers().stream()
                .filter(m -> !parents.contains(m.memberId()))
                .map(m -> new GroupMembership<>(MemberId.fromUserId(m.memberId()), m.joinedAt()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isLastParent(UserId userId) {
        return isLastOwner(userId);
    }

    public void addParent(UserId parent) {
        Assert.notNull(parent, "Parent UserId is required");
        addOwner(parent);
        // If already a member (was a child), skip adding membership to avoid duplicate
        if (!hasMember(parent)) {
            addMember(parent);
        }
    }

    public void removeParent(UserId parent) {
        Assert.notNull(parent, "Parent UserId is required");
        // Remove from owners first so the subsequent removeMember call is not blocked by the owner guard
        removeOwner(parent);
        removeMember(parent);
    }

    public void addChild(MemberId child) {
        Assert.notNull(child, "Child MemberId is required");
        UserId childUserId = child.toUserId();
        if (isOwner(childUserId)) {
            throw new MemberAlreadyInGroupException(child);
        }
        addMember(childUserId);
    }

    public void removeChild(MemberId child) {
        Assert.notNull(child, "Child MemberId is required");
        removeMember(child.toUserId());
    }
}
