package com.klabis.usergroups.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AggregateRoot
public abstract class UserGroup extends KlabisAggregateRoot<UserGroup, UserGroupId> {

    @Identity
    private final UserGroupId id;
    private String name;
    private final Set<MemberId> owners;
    private final Set<GroupMembership> members;

    protected UserGroup(UserGroupId id, String name, Set<MemberId> owners, Set<GroupMembership> members) {
        Assert.notNull(id, "UserGroup id is required");
        Assert.hasText(name, "UserGroup name is required");
        Assert.notEmpty(owners, "UserGroup must have at least one owner");
        this.id = id;
        this.name = name;
        this.owners = new HashSet<>(owners);
        this.members = new HashSet<>(members);
    }

    public void rename(String newName) {
        Assert.hasText(newName, "UserGroup name is required");
        this.name = newName;
    }

    public void addMember(MemberId memberId) {
        if (this instanceof WithInvitations) {
            throw new DirectMemberAdditionNotAllowedException();
        }
        addMemberInternal(memberId);
    }

    protected void addMemberInternal(MemberId memberId) {
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

    public boolean isOwner(MemberId memberId) {
        return owners.contains(memberId);
    }

    public boolean hasMember(MemberId memberId) {
        return members.stream().anyMatch(m -> m.memberId().equals(memberId));
    }

    @Override
    public UserGroupId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<MemberId> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    public Set<GroupMembership> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    static final class MemberAlreadyInGroupException extends BusinessRuleViolationException {
        MemberAlreadyInGroupException(MemberId memberId) {
            super("Member %s is already in the group".formatted(memberId));
        }
    }

    static final class OwnerCannotBeRemovedFromGroupException extends BusinessRuleViolationException {
        OwnerCannotBeRemovedFromGroupException(MemberId memberId) {
            super("Owner %s cannot be removed from the group".formatted(memberId));
        }
    }

    static final class MemberNotInGroupException extends BusinessRuleViolationException {
        MemberNotInGroupException(MemberId memberId) {
            super("Member %s is not in the group".formatted(memberId));
        }
    }
}
