package com.klabis.groups.freegroup.application;

import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.GroupOwnershipRequiredException;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class FreeGroupManagementService implements FreeGroupManagementPort {

    private final FreeGroupRepository freeGroupRepository;

    FreeGroupManagementService(FreeGroupRepository freeGroupRepository) {
        this.freeGroupRepository = freeGroupRepository;
    }

    @Transactional
    @Override
    public FreeGroup createGroup(String name, MemberId creatorMemberId) {
        FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup(name, creatorMemberId));
        return freeGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public FreeGroup getGroup(FreeGroupId id) {
        return loadGroup(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FreeGroup> listGroupsForMember(MemberId memberId) {
        return freeGroupRepository.findAll(FreeGroupFilter.all().withOwnerOrMemberIs(memberId));
    }

    @Transactional
    @Override
    public FreeGroup renameGroup(FreeGroupId id, String newName, MemberId actingMember) {
        FreeGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.rename(newName);
        return freeGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteGroup(FreeGroupId id, MemberId actingMember) {
        FreeGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        freeGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addOwner(FreeGroupId id, MemberId memberId, MemberId actingMember) {
        FreeGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.addOwner(memberId);
        freeGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeOwner(FreeGroupId id, MemberId memberId, MemberId actingMember) {
        FreeGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.removeOwner(memberId);
        freeGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeMember(FreeGroupId id, MemberId memberId, MemberId actingMember) {
        FreeGroup group = loadGroup(id);
        requireOwnership(group, actingMember);
        group.removeMember(memberId);
        freeGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void inviteMember(FreeGroupId id, MemberId invitedBy, MemberId target) {
        FreeGroup group = loadGroup(id);
        group.invite(invitedBy, target);
        freeGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void acceptInvitation(FreeGroupId id, InvitationId invitationId, MemberId acceptingMember) {
        FreeGroup group = loadGroup(id);
        group.acceptInvitation(invitationId, acceptingMember);
        freeGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void rejectInvitation(FreeGroupId id, InvitationId invitationId, MemberId rejectingMember) {
        FreeGroup group = loadGroup(id);
        group.rejectInvitation(invitationId, rejectingMember);
        freeGroupRepository.save(group);
    }

    private FreeGroup loadGroup(FreeGroupId id) {
        return freeGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException("Members", id));
    }

    private void requireOwnership(FreeGroup group, MemberId actingMember) {
        if (!group.isOwner(actingMember)) {
            throw new GroupOwnershipRequiredException(actingMember, group.getId());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<FreeGroup> getGroupsWithPendingInvitations(MemberId memberId) {
        return freeGroupRepository.findAll(FreeGroupFilter.all().withPendingInvitationFor(memberId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PendingInvitationView> getPendingInvitationsForMember(MemberId memberId) {
        return freeGroupRepository.findAll(FreeGroupFilter.all().withPendingInvitationFor(memberId)).stream()
                .flatMap(group -> group.getPendingInvitations().stream()
                        .filter(inv -> inv.isForUser(memberId.toUserId()))
                        .map(inv -> new PendingInvitationView(group.getId(), group.getName(), inv)))
                .toList();
    }
}
