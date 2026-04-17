package com.klabis.members.application;

import com.klabis.members.MemberSuspensionRequestedEvent;

import java.util.List;

/**
 * Thrown when suspending a member who is the sole owner of at least one group.
 * The caller must ensure the member designates a successor (or dissolves the group) before suspension.
 */
public class MemberIsLastGroupOwnerException extends RuntimeException {

    private final List<MemberSuspensionRequestedEvent.BlockingGroup> groups;

    public MemberIsLastGroupOwnerException(List<MemberSuspensionRequestedEvent.BlockingGroup> groups) {
        super("Member is the last owner of %d group(s) — designate a successor before suspension".formatted(groups.size()));
        this.groups = groups;
    }

    public List<MemberSuspensionRequestedEvent.BlockingGroup> getGroups() {
        return groups;
    }
}
