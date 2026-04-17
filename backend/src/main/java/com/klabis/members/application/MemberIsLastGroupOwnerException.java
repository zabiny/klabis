package com.klabis.members.application;

import com.klabis.groups.LastOwnershipChecker;

import java.util.List;

/**
 * Thrown when suspending a member who is the sole owner of at least one group.
 * The caller must ensure the member designates a successor (or dissolves the group) before suspension.
 */
public class MemberIsLastGroupOwnerException extends RuntimeException {

    private final List<LastOwnershipChecker.OwnedGroupInfo> groups;

    public MemberIsLastGroupOwnerException(List<LastOwnershipChecker.OwnedGroupInfo> groups) {
        super("Member is the last owner of %d group(s) — designate a successor before suspension".formatted(groups.size()));
        this.groups = groups;
    }

    public List<LastOwnershipChecker.OwnedGroupInfo> getGroups() {
        return groups;
    }
}
