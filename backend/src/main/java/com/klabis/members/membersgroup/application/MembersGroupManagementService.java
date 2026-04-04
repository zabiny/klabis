package com.klabis.members.membersgroup.application;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.MemberId;
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
        return membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembersGroup> listGroupsForMember(MemberId memberId) {
        return membersGroupRepository.findGroupsForMember(memberId);
    }

    @Transactional
    @Override
    public MembersGroup renameGroup(MembersGroupId id, String newName) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.rename(newName);
        return membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteGroup(MembersGroupId id) {
        membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        membersGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addOwner(MembersGroupId id, MemberId memberId) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.addOwner(memberId);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeOwner(MembersGroupId id, MemberId memberId) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.removeOwner(memberId);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeMember(MembersGroupId id, MemberId memberId) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.removeMember(memberId);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void inviteMember(MembersGroupId id, MemberId invitedBy, MemberId target) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.invite(invitedBy, target);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void acceptInvitation(MembersGroupId id, InvitationId invitationId, MemberId acceptingMember) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.acceptInvitation(invitationId, acceptingMember);
        membersGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void rejectInvitation(MembersGroupId id, InvitationId invitationId, MemberId rejectingMember) {
        MembersGroup group = membersGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.rejectInvitation(invitationId, rejectingMember);
        membersGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembersGroup> getGroupsWithPendingInvitations(MemberId memberId) {
        return membersGroupRepository.findGroupsWithPendingInvitationsForMember(memberId);
    }
}
