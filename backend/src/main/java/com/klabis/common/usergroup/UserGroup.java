package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Low-level building block representing membership and ownership state of a group.
 * <p>
 * Not an aggregate root — identity is owned by the aggregate root that embeds this.
 * Contains no authorization logic and no framework dependencies.
 * Aggregate roots extend this class and add identity, domain events, and authorization.
 */
public class UserGroup {

    private String name;
    private final Set<UserId> owners;
    private final Set<GroupMembership> members;

    protected UserGroup(String name, Set<UserId> owners, Set<GroupMembership> members) {
        Assert.hasText(name, "UserGroup name is required");
        Assert.notEmpty(owners, "UserGroup must have at least one owner");
        this.name = name;
        this.owners = new HashSet<>(owners);
        this.members = new HashSet<>(members);
    }

    public static UserGroup create(String name, UserId owner) {
        Assert.notNull(owner, "Owner is required");
        return new UserGroup(name, Set.of(owner), Set.of(GroupMembership.of(owner)));
    }

    public static UserGroup reconstruct(String name, Set<UserId> owners, Set<GroupMembership> members) {
        return new UserGroup(name, owners, members);
    }

    public void rename(String newName) {
        Assert.hasText(newName, "UserGroup name is required");
        this.name = newName;
    }

    public void addMember(UserId userId) {
        Assert.notNull(userId, "UserId is required");
        boolean alreadyMember = members.stream().anyMatch(m -> m.userId().equals(userId));
        if (alreadyMember) {
            throw new MemberAlreadyInGroupException(userId);
        }
        members.add(GroupMembership.of(userId));
    }

    public void removeMember(UserId userId) {
        Assert.notNull(userId, "UserId is required");
        if (owners.contains(userId)) {
            throw new OwnerCannotBeRemovedFromGroupException(userId);
        }
        boolean removed = members.removeIf(m -> m.userId().equals(userId));
        if (!removed) {
            throw new MemberNotInGroupException(userId);
        }
    }

    /**
     * Adds the user to the owners set only. Callers that also want the user to be a member
     * must call {@link #addMember(UserId)} separately (with a {@link #hasMember(UserId)} guard
     * if the user may already be a member). {@code FamilyGroup} does this; {@code TrainingGroup}
     * and {@code MembersGroup} do not.
     */
    public void addOwner(UserId userId) {
        Assert.notNull(userId, "UserId is required");
        owners.add(userId);
    }

    public void removeOwner(UserId userId) {
        Assert.notNull(userId, "UserId is required");
        if (isLastOwner(userId)) {
            throw new CannotRemoveLastOwnerException(userId);
        }
        owners.remove(userId);
    }

    public boolean isLastOwner(UserId userId) {
        return owners.size() == 1 && owners.contains(userId);
    }

    public boolean isOwner(UserId userId) {
        return owners.contains(userId);
    }

    public boolean hasMember(UserId userId) {
        return members.stream().anyMatch(m -> m.userId().equals(userId));
    }

    public String getName() {
        return name;
    }

    public Set<UserId> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    public Set<GroupMembership> getMembers() {
        return Collections.unmodifiableSet(members);
    }

}
