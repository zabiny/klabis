package com.klabis.members.membersgroup.application;

import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.MemberId;
import com.klabis.members.groups.domain.MembersGroupFilter;
import com.klabis.members.membersgroup.domain.GroupOwnershipRequiredException;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import com.klabis.members.membersgroup.domain.MembersGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class MembersGroupManagementService implements MembersGroupManagementPort {

    private final MembersGroupRepository membersGroupRepository;

    MembersGroupManagementService(MembersGroupRepository membersGroupRepository) {
        this.membersGroupRepository = membersGroupRepository;
    }

    @Transactional
    @Override
    public MembersGroup createGroup(String name, MemberId creatorMemberId) {
        MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup(name, creatorMemberId));
        return membersGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public MembersGroup getGroup(MembersGroupId id) {
        return loadGroup(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembersGroup> listGroupsForMember(MemberId memberId) {
        return membersGroupRepository.findAll(MembersGroupFilter.all().withOwnerOrMemberIs(memberId));
    }

    @Transactional
    @Override
    public MembersGroup renameGroup(MembersGroupId id, String newName, MemberId actingMember) {
        MembersGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.rename(newName);
        return membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteGroup(MembersGroupId id, MemberId actingMember) {
        MembersGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        membersGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addOwner(MembersGroupId id, MemberId memberId, MemberId actingMember) {
        MembersGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.addOwner(memberId);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeOwner(MembersGroupId id, MemberId memberId, MemberId actingMember) {
        MembersGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.removeOwner(memberId);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeMember(MembersGroupId id, MemberId memberId, MemberId actingMember) {
        MembersGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.removeMember(memberId);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void inviteMember(MembersGroupId id, MemberId invitedBy, MemberId target) {
        MembersGroup group = loadGroup(id);
        group.invite(invitedBy, target);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void acceptInvitation(MembersGroupId id, InvitationId invitationId, MemberId acceptingMember) {
        MembersGroup group = loadGroup(id);
        group.acceptInvitation(invitationId, acceptingMember);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void rejectInvitation(MembersGroupId id, InvitationId invitationId, MemberId rejectingMember) {
        MembersGroup group = loadGroup(id);
        group.rejectInvitation(invitationId, rejectingMember);
        membersGroupRepository.save(group);
    }

    private MembersGroup loadGroup(MembersGroupId id) {
        return membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException("Members", id));
    }

    private void requireOwnership(MembersGroup group, MemberId actingMember) {
        if (!group.isOwner(actingMember)) {
            throw new GroupOwnershipRequiredException(actingMember, group.getId());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembersGroup> getGroupsWithPendingInvitations(MemberId memberId) {
        return membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(memberId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PendingInvitationView> getPendingInvitationsForMember(MemberId memberId) {
        return membersGroupRepository.findAll(MembersGroupFilter.all().withPendingInvitationFor(memberId)).stream()
                .flatMap(group -> group.getPendingInvitations().stream()
                        .filter(inv -> inv.isForUser(memberId.toUserId()))
                        .map(inv -> new PendingInvitationView(group.getId(), group.getName(), inv)))
                .toList();
    }
}
