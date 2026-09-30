package com.klabis.members.legalguardiangroup.application;

import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupWithoutGuardianException;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class LegalGuardianGroupService implements LegalGuardianGroupPort {

    private final LegalGuardianGroupRepository groupRepository;
    private final GuardianContactResolver guardianContactResolver;
    private final MemberRepository memberRepository;

    LegalGuardianGroupService(LegalGuardianGroupRepository groupRepository,
                              GuardianContactResolver guardianContactResolver,
                              MemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.guardianContactResolver = guardianContactResolver;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<LegalGuardianGroup> listGroups() {
        return groupRepository.findAll(LegalGuardianGroupFilter.all());
    }

    @Transactional(readOnly = true)
    @Override
    public LegalGuardianGroup getGroup(LegalGuardianGroupId id) {
        return loadGroup(id);
    }

    @Transactional
    @Override
    public void changeGroupGuardians(LegalGuardianGroupId id, Set<UserId> guardianIds) {
        LegalGuardianGroup group = loadGroup(id);
        Set<Guardian> guardians = resolveGuardians(guardianIds);

        Optional<LegalGuardianGroup> existing = findGroupWithGuardians(guardianIds)
                .filter(other -> !other.getId().equals(id));
        if (existing.isPresent()) {
            LegalGuardianGroup target = existing.get();
            target.takeMinorsFrom(group);
            groupRepository.save(target);
            groupRepository.delete(group.getId());
        } else {
            group.changeGuardians(guardians);
            groupRepository.save(group);
        }
    }

    @Transactional
    @Override
    public void setGuardiansOf(MemberId minorId, Set<UserId> guardianIds) {
        Set<Guardian> guardians = resolveGuardians(guardianIds);
        Minor minor = loadMinor(minorId);
        Optional<LegalGuardianGroup> current = groupRepository.findOne(
                LegalGuardianGroupFilter.all().withMinorIs(minorId.toUserId()));
        Optional<LegalGuardianGroup> target = findGroupWithGuardians(guardianIds);

        if (target.isPresent() && current.map(c -> c.getId().equals(target.get().getId())).orElse(false)) {
            return;
        }

        if (target.isPresent()) {
            leaveCurrentGroup(current, minorId);
            target.get().addMinor(minor);
            groupRepository.save(target.get());
        } else if (current.isPresent() && current.get().getMinors().size() == 1) {
            current.get().changeGuardians(guardians);
            groupRepository.save(current.get());
        } else {
            leaveCurrentGroup(current, minorId);
            groupRepository.save(LegalGuardianGroup.create(guardians, minor));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Set<UserId> guardiansOf(MemberId minor) {
        return groupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(minor.toUserId()))
                .map(LegalGuardianGroup::getGuardians)
                .orElse(Set.of());
    }

    private void leaveCurrentGroup(Optional<LegalGuardianGroup> current, MemberId minorId) {
        current.ifPresent(group -> {
            group.removeMinor(minorId);
            if (group.hasMinors()) {
                groupRepository.save(group);
            } else {
                groupRepository.delete(group.getId());
            }
        });
    }

    private Optional<LegalGuardianGroup> findGroupWithGuardians(Set<UserId> guardianIds) {
        return groupRepository.findOne(LegalGuardianGroupFilter.all().withGuardiansAre(guardianIds));
    }

    private Set<Guardian> resolveGuardians(Set<UserId> guardianIds) {
        if (guardianIds == null || guardianIds.isEmpty()) {
            throw new LegalGuardianGroupWithoutGuardianException();
        }
        Map<UserId, GuardianContact> contacts = guardianContactResolver.resolve(guardianIds).stream()
                .collect(Collectors.toMap(GuardianContact::userId, Function.identity()));
        return guardianIds.stream().map(userId -> {
            GuardianContact contact = contacts.get(userId);
            if (contact == null) {
                throw new GuardianNotFoundException(userId);
            }
            return new Guardian(userId, contact.lastName());
        }).collect(Collectors.toUnmodifiableSet());
    }

    private Minor loadMinor(MemberId minorId) {
        Member member = memberRepository.findById(minorId).orElseThrow(() -> new MemberNotFoundException(minorId));
        return new Minor(minorId, member.getDateOfBirth());
    }

    private LegalGuardianGroup loadGroup(LegalGuardianGroupId id) {
        return groupRepository.findById(id).orElseThrow(() -> new GroupNotFoundException("LegalGuardian", id));
    }
}
