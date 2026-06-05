package com.klabis.groups.common.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.members.MemberId;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for all member-based groups in the domain.
 * <p>
 * Encapsulates the common membership and ownership logic for groups whose members
 * are identified by {@link MemberId}. Subclasses add identity, domain events,
 * and group-specific business rules (e.g. invitation flow, age constraints).
 */
public abstract class MemberGroup<A extends MemberGroup<A, ID>, ID> extends KlabisAggregateRoot<A, ID> {

    private String name;
    private final Set<MemberId> owners;
    private final Set<GroupMembership> members;

    protected MemberGroup(String name, Set<MemberId> owners, Set<GroupMembership> members) {
        Assert.hasText(name, "Group name is required");
        Assert.notEmpty(owners, "Group must have at least one owner");
        this.name = name;
        this.owners = new HashSet<>(owners);
        this.members = new HashSet<>(members);
    }

    public void rename(String newName) {
        Assert.hasText(newName, "Group name is required");
        this.name = newName;
    }

    public void addOwner(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        owners.add(memberId);
    }

    public void removeOwner(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        if (isLastOwner(memberId)) {
            throw new CannotRemoveLastOwnerException(memberId);
        }
        owners.remove(memberId);
    }

    public boolean isOwner(MemberId memberId) {
        return owners.contains(memberId);
    }

    public boolean isLastOwner(MemberId memberId) {
        return owners.size() == 1 && owners.contains(memberId);
    }

    public Set<MemberId> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    public void addMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        boolean alreadyMember = members.stream().anyMatch(m -> m.memberId().equals(memberId));
        if (alreadyMember) {
            throw new MemberAlreadyInGroupException(memberId);
        }
        members.add(GroupMembership.of(memberId));
    }

    public void removeMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        if (owners.contains(memberId)) {
            throw new OwnerCannotBeRemovedFromGroupException(memberId);
        }
        boolean removed = members.removeIf(m -> m.memberId().equals(memberId));
        if (!removed) {
            throw new MemberNotInGroupException(memberId);
        }
    }

    public boolean hasMember(MemberId memberId) {
        return members.stream().anyMatch(m -> m.memberId().equals(memberId));
    }

    public Set<GroupMembership> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public String getName() {
        return name;
    }
}
