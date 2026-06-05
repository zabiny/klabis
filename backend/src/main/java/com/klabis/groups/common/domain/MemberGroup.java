package com.klabis.groups.common.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.members.MemberId;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final Set<MemberId> memberIds;

    protected MemberGroup(String name, Set<MemberId> owners, Set<GroupMembership> members) {
        Assert.hasText(name, "Group name is required");
        Assert.notEmpty(owners, "Group must have at least one owner");
        this.name = name;
        this.owners = new HashSet<>(owners);
        this.members = new HashSet<>(members);
        this.memberIds = members.stream()
                .map(GroupMembership::memberId)
                .collect(Collectors.toCollection(HashSet::new));
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

    protected void addMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        if (memberIds.contains(memberId)) {
            throw new MemberAlreadyInGroupException(memberId);
        }
        members.add(GroupMembership.of(memberId));
        memberIds.add(memberId);
    }

    protected void removeMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        if (owners.contains(memberId)) {
            throw new OwnerCannotBeRemovedFromGroupException(memberId);
        }
        boolean removed = members.removeIf(m -> m.memberId().equals(memberId));
        if (!removed) {
            throw new MemberNotInGroupException(memberId);
        }
        memberIds.remove(memberId);
    }

    public boolean hasMember(MemberId memberId) {
        return memberIds.contains(memberId);
    }

    public Set<GroupMembership> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public String getName() {
        return name;
    }
}
