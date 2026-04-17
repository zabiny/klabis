package com.klabis.groups.freegroup.application;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface FreeGroupManagementPort {

    FreeGroup createGroup(String name, MemberId creatorMemberId);

    FreeGroup getGroup(FreeGroupId id);

    List<FreeGroup> listGroupsForMember(MemberId memberId);

    FreeGroup renameGroup(FreeGroupId id, String newName, MemberId actingMember);

    void deleteGroup(FreeGroupId id, MemberId actingMember);

    void addOwner(FreeGroupId id, MemberId memberId, MemberId actingMember);

    void removeOwner(FreeGroupId id, MemberId memberId, MemberId actingMember);

    void removeMember(FreeGroupId id, MemberId memberId, MemberId actingMember);

    void inviteMember(FreeGroupId id, MemberId invitedBy, MemberId target);

    void acceptInvitation(FreeGroupId id, InvitationId invitationId, MemberId acceptingMember);

    void rejectInvitation(FreeGroupId id, InvitationId invitationId, MemberId rejectingMember);

    List<FreeGroup> getGroupsWithPendingInvitations(MemberId memberId);

    List<PendingInvitationView> getPendingInvitationsForMember(MemberId memberId);
}
