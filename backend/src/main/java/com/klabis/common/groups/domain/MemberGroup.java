package com.klabis.common.groups.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for all member-based groups in the domain.
 * <p>
 * Encapsulates the common membership and ownership logic for groups whose members
 * are identified by the member id type {@code M}. Owners and members are disjoint:
 * nobody owns and belongs to the same group at once. Subclasses add identity, domain events,
 * and group-specific business rules (e.g. invitation flow, age constraints).
 */
public abstract class MemberGroup<A extends MemberGroup<A, ID, M>, ID, M> extends KlabisAggregateRoot<A, ID> {

    private String name;
    private final Set<M> owners;
    private final Set<GroupMembership<M>> members;
    private final Set<M> memberIds;

    protected MemberGroup(String name, Set<M> owners, Set<GroupMembership<M>> members) {
        Assert.hasText(name, "Group name is required");
        Assert.notEmpty(owners, "Group must have at least one owner");
        this.name = name;
        this.owners = new HashSet<>(owners);
        this.members = members.stream()
                .filter(m -> !owners.contains(m.memberId()))
                .collect(Collectors.toCollection(HashSet::new));
        this.memberIds = this.members.stream()
                .map(GroupMembership::memberId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public void rename(String newName) {
        Assert.hasText(newName, "Group name is required");
        this.name = newName;
    }

    /**
     * Promoting a member moves them out of the member list — an owner never belongs to the group.
     */
    public void addOwner(M memberId) {
        Assert.notNull(memberId, "Member id is required");
        members.removeIf(m -> m.memberId().equals(memberId));
        memberIds.remove(memberId);
        owners.add(memberId);
    }

    /**
     * The person leaves the group entirely; giving up ownership never downgrades them to a member.
     */
    public void removeOwner(M memberId) {
        Assert.notNull(memberId, "Member id is required");
        if (isLastOwner(memberId)) {
            throw new CannotRemoveLastOwnerException(memberId);
        }
        owners.remove(memberId);
    }

    public boolean isOwner(M memberId) {
        return owners.contains(memberId);
    }

    public boolean isLastOwner(M memberId) {
        return owners.size() == 1 && owners.contains(memberId);
    }

    public Set<M> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    protected void addMember(M memberId) {
        Assert.notNull(memberId, "Member id is required");
        if (owners.contains(memberId)) {
            throw new OwnerCannotBeMemberException(memberId);
        }
        if (memberIds.contains(memberId)) {
            throw new MemberAlreadyInGroupException(memberId);
        }
        members.add(GroupMembership.of(memberId));
        memberIds.add(memberId);
    }

    protected void removeMember(M memberId) {
        Assert.notNull(memberId, "Member id is required");
        boolean removed = members.removeIf(m -> m.memberId().equals(memberId));
        if (!removed) {
            throw new MemberNotInGroupException(memberId);
        }
        memberIds.remove(memberId);
    }

    public boolean hasMember(M memberId) {
        return memberIds.contains(memberId);
    }

    public Set<GroupMembership<M>> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public String getName() {
        return name;
    }
}
