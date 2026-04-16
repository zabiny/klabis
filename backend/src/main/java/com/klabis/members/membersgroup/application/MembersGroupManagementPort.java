package com.klabis.members.membersgroup.application;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.MemberId;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface MembersGroupManagementPort {

    MembersGroup createGroup(String name, MemberId creatorMemberId);

    MembersGroup getGroup(MembersGroupId id);

    List<MembersGroup> listGroupsForMember(MemberId memberId);

    MembersGroup renameGroup(MembersGroupId id, String newName, MemberId actingMember);

    void deleteGroup(MembersGroupId id, MemberId actingMember);

    void addOwner(MembersGroupId id, MemberId memberId, MemberId actingMember);

    void removeOwner(MembersGroupId id, MemberId memberId, MemberId actingMember);

    void removeMember(MembersGroupId id, MemberId memberId, MemberId actingMember);

    void inviteMember(MembersGroupId id, MemberId invitedBy, MemberId target);

    void acceptInvitation(MembersGroupId id, InvitationId invitationId, MemberId acceptingMember);

    void rejectInvitation(MembersGroupId id, InvitationId invitationId, MemberId rejectingMember);

    List<MembersGroup> getGroupsWithPendingInvitations(MemberId memberId);

    List<PendingInvitationView> getPendingInvitationsForMember(MemberId memberId);
}
