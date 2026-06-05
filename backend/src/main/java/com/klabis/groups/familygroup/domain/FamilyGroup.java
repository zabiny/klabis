package com.klabis.groups.familygroup.domain;

import com.klabis.common.domain.AuditMetadata;
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
public class FamilyGroup extends MemberGroup<FamilyGroup, FamilyGroupId> {

    public static final String TYPE_DISCRIMINATOR = "FAMILY";

    @Identity
    private final FamilyGroupId id;

    private FamilyGroup(FamilyGroupId id, String name, Set<MemberId> parents, Set<GroupMembership> members) {
        super(name, parents, members);
        Assert.notNull(id, "FamilyGroupId is required");
        this.id = id;
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
        // Parent is both owner and member from the start
        return new FamilyGroup(id, command.name(), Set.of(command.parent()),
                Set.of(GroupMembership.of(command.parent())));
    }

    public static FamilyGroup reconstruct(FamilyGroupId id, String name, Set<MemberId> parents,
                                          Set<GroupMembership> members, AuditMetadata auditMetadata) {
        FamilyGroup group = new FamilyGroup(id, name, parents, members);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public FamilyGroupId getId() {
        return id;
    }

    public Set<MemberId> getParents() {
        return getOwners();
    }

    public Set<GroupMembership> getChildren() {
        Set<MemberId> parents = getParents();
        return getMembers().stream()
                .filter(m -> !parents.contains(m.memberId()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isLastParent(MemberId memberId) {
        return isLastOwner(memberId);
    }

    public void addParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        addOwner(parent);
        // If already a member (was a child), skip adding membership to avoid duplicate
        if (!hasMember(parent)) {
            addMember(parent);
        }
    }

    public void removeParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        // Remove from owners first so the subsequent removeMember call is not blocked by the owner guard
        removeOwner(parent);
        removeMember(parent);
    }

    public void addChild(MemberId child) {
        Assert.notNull(child, "Child MemberId is required");
        if (isOwner(child)) {
            throw new MemberAlreadyInGroupException(child);
        }
        addMember(child);
    }

    public void removeChild(MemberId child) {
        Assert.notNull(child, "Child MemberId is required");
        removeMember(child);
    }
}
