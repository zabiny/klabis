package com.klabis.members;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MemberSuspensionRequestedEvent {

    private final MemberId memberId;
    private final List<BlockingGroup> blockingGroups = new ArrayList<>();

    public MemberSuspensionRequestedEvent(MemberId memberId) {
        this.memberId = memberId;
    }

    public MemberId memberId() {
        return memberId;
    }

    public void addBlockingGroup(String groupId, String groupName, String groupType) {
        blockingGroups.add(new BlockingGroup(groupId, groupName, groupType));
    }

    public List<BlockingGroup> blockingGroups() {
        return Collections.unmodifiableList(blockingGroups);
    }

    public record BlockingGroup(String groupId, String groupName, String groupType) {}
}
