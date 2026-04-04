package com.klabis.usergroups.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;

public class FamilyGroup extends UserGroup {

    public static final String TYPE_DISCRIMINATOR = "FAMILY";

    private FamilyGroup(UserGroupId id, String name, Set<MemberId> owners, Set<GroupMembership> members) {
        super(id, name, owners, members);
    }

    @Override
    public String typeDiscriminator() {
        return TYPE_DISCRIMINATOR;
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
        UserGroupId id = new UserGroupId(UUID.randomUUID());
        FamilyGroup group = new FamilyGroup(id, command.name(), command.parents(), Set.of());
        command.parents().forEach(group::addMemberInternal);
        command.initialMembers().forEach(group::addMemberInternal);
        return group;
    }

    public static FamilyGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                          Set<GroupMembership> members, AuditMetadata auditMetadata) {
        FamilyGroup group = new FamilyGroup(id, name, owners, members);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    public Set<MemberId> getParents() {
        return getOwners();
    }

    public void addParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        addOwner(parent);
        addMemberInternal(parent);
    }

    public void removeParent(MemberId parent) {
        Assert.notNull(parent, "Parent MemberId is required");
        // removeOwner checks last-owner invariant before removing from owners set
        removeOwner(parent);
        // after ownership is removed, removeMember proceeds without the owner guard
        removeMember(parent);
    }
}
