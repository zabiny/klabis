package com.klabis.members;

import org.jmolecules.event.annotation.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Published before a member is suspended so other modules can veto the operation.
 * Listeners call {@link #addBlockingGroup} if the member still owns groups that must
 * be reassigned first; the caller inspects {@link #blockingGroups()} after publishing
 * and aborts suspension if any are present.
 *
 * <p>The veto collector relies on Spring's default synchronous event dispatch
 * ({@code SimpleApplicationEventMulticaster}). An async multicaster would break
 * the contract silently.
 */
@DomainEvent
public final class MemberSuspensionRequestedEvent {

    private final MemberId memberId;
    private final List<OwnedGroup> blockingGroups = new ArrayList<>();

    public MemberSuspensionRequestedEvent(MemberId memberId) {
        this.memberId = memberId;
    }

    public MemberId memberId() {
        return memberId;
    }

    public void addBlockingGroup(String groupId, String groupName, String groupType) {
        blockingGroups.add(new OwnedGroup(groupId, groupName, groupType));
    }

    public List<OwnedGroup> blockingGroups() {
        return Collections.unmodifiableList(blockingGroups);
    }
}
