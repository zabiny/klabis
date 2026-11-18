package com.klabis.members.application;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.BirthNumberAccessedEvent;
import com.klabis.members.MemberId;
import com.klabis.members.MonetaryAmount;
import com.klabis.members.OwnedGroup;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ManagementService implements ManagementPort {

    private static final Logger log = LoggerFactory.getLogger(ManagementService.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberFinancialStatePort memberFinancialStatePort;
    private final List<MemberOwnedGroupsPort> memberOwnedGroupsPorts;
    private final MemberCompletenessPort memberCompletenessPort;
    private final LegalGuardianCascadePort legalGuardianCascadePort;
    private final AuthorizationEvaluator authorizationEvaluator;

    public ManagementService(MemberRepository memberRepository, UserService userService,
                             ApplicationEventPublisher eventPublisher,
                             Optional<MemberFinancialStatePort> memberFinancialStatePort,
                             List<MemberOwnedGroupsPort> memberOwnedGroupsPorts,
                             MemberCompletenessPort memberCompletenessPort,
                             LegalGuardianCascadePort legalGuardianCascadePort,
                             AuthorizationEvaluator authorizationEvaluator) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.memberFinancialStatePort = memberFinancialStatePort.orElseGet(NoOpMemberFinancialStatePort::new);
        this.memberOwnedGroupsPorts = memberOwnedGroupsPorts;
        this.memberCompletenessPort = memberCompletenessPort;
        this.legalGuardianCascadePort = legalGuardianCascadePort;
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Member> listMembers(MemberFilter filter, Pageable pageable) {
        return memberRepository.findAll(filter, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Member> listActiveMembers() {
        return memberRepository.findAll(MemberFilter.activeOnly());
    }

    @Override
    public Member.UpdateMember prefilledUpdateCommand(MemberId memberId) {
        return Member.UpdateMember.from(loadMember(memberId));
    }

    @Transactional
    @Override
    public Member updateMember(MemberId memberId, Member.UpdateMember command) {
        Member member = loadMember(memberId);
        member.update(command, memberCompletenessPort.guardianContactsOf(member));
        Member saved = memberRepository.save(member);
        log.info("Member updated: memberId={}", memberId);
        return saved;
    }

    @Transactional
    @Override
    public Member syncMemberFromOris(MemberId memberId, Member.SyncFromOris command) {
        Member member = loadMember(memberId);
        member.syncFromOris(command);
        member.recordMissingData(memberCompletenessPort.missingData(member));
        Member saved = memberRepository.save(member);
        log.info("Member synced from ORIS: memberId={}", memberId);
        return saved;
    }

    @Transactional
    @Override
    public Member suspendMember(MemberId memberId, Member.SuspendMembership command,
                                Set<LegalGuardianGroupId> cascadeGroupIds) {
        Member member = loadMember(memberId);
        List<Member> minors = loadActiveCascadedMinors(memberId, cascadeGroupIds);

        Blockers own = blockersOf(memberId, cascadeGroupIds);
        List<SuspensionBlockedException.DependentBlockers> dependents = minors.stream()
                .map(this::dependentBlockersOf)
                .filter(dependent -> !dependent.blockingGroups().isEmpty() || dependent.debtSnapshot() != null)
                .toList();
        if (!own.isEmpty() || !dependents.isEmpty()) {
            throw new SuspensionBlockedException(own.groups(), own.debt(), dependents);
        }

        log.info("Processing membership suspension: memberId={}, reason={}, cascadedMinors={}",
                memberId, command.reason(), minors.size());

        Member saved = suspend(member, command);
        minors.forEach(minor -> suspend(minor, command));

        return saved;
    }

    private Member suspend(Member member, Member.SuspendMembership command) {
        member.suspend(command);

        Member saved = memberRepository.save(member);

        userService.suspendUser(member.getId().toUserId());

        log.info("Membership suspended: memberId={}, suspendedAt={}, suspendedBy={}",
                saved.getId(), saved.getSuspendedAt(), command.suspendedBy());

        return saved;
    }

    private List<Member> loadActiveCascadedMinors(MemberId guardian, Set<LegalGuardianGroupId> cascadeGroupIds) {
        if (cascadeGroupIds.isEmpty()) {
            return List.of();
        }
        List<Member> minors = legalGuardianCascadePort.minorsOfSoleGuardianGroups(guardian, cascadeGroupIds).stream()
                .map(this::loadMember)
                .toList();
        minors.forEach(minor -> {
            if (!authorizationEvaluator.has(Authority.MEMBERS_MANAGE, TargetRef.member(minor.getId().uuid()))) {
                throw new InsufficientAuthorityException(Authority.MEMBERS_MANAGE.name());
            }
        });
        return minors.stream().filter(Member::isActive).toList();
    }

    private SuspensionBlockedException.DependentBlockers dependentBlockersOf(Member minor) {
        Blockers blockers = blockersOf(minor.getId(), Set.of());
        return new SuspensionBlockedException.DependentBlockers(minor.getId(),
                minor.getPersonalInformation().getFullName(), blockers.groups(), blockers.debt());
    }

    /**
     * Legal guardian groups that are suspended along with the member do not block: their minors are not left
     * without a guardian.
     */
    private Blockers blockersOf(MemberId memberId, Set<LegalGuardianGroupId> cascadeGroupIds) {
        Set<String> cascaded = cascadeGroupIds.stream().map(id -> id.uuid().toString()).collect(Collectors.toSet());
        List<OwnedGroup> blockingGroups = memberOwnedGroupsPorts.stream()
                .flatMap(port -> port.findGroupsBlockingSuspension(memberId).stream())
                .filter(group -> !cascaded.contains(group.groupId()))
                .toList();
        var snapshot = memberFinancialStatePort.getFinancialSnapshot(memberId);
        return new Blockers(blockingGroups, snapshot.hasOutstandingDebt() ? snapshot : null);
    }

    private record Blockers(List<OwnedGroup> groups, MemberFinancialStatePort.MemberFinancialSnapshot debt) {
        boolean isEmpty() {
            return groups.isEmpty() && debt == null;
        }
    }

    @Transactional
    @Override
    public Member resumeMember(MemberId memberId, Member.ResumeMembership command) {
        Member member = loadMember(memberId);

        log.info("Processing membership resume: memberId={}", memberId);

        member.resume(command);

        Member saved = memberRepository.save(member);

        userService.resumeUser(member.getId().toUserId());

        log.info("Membership resumed: memberId={}, resumedBy={}", saved.getId(), command.resumedBy());

        return saved;
    }

    @Transactional
    @Override
    public Member getMemberAndRecordView(MemberId memberId, UserId viewedBy, MemberViewAccess access) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (!access.suspendedVisible() && !member.isActive()) {
            throw new MemberNotFoundException(memberId);
        }

        if (member.getBirthNumber() != null && access.birthNumberVisible()) {
            eventPublisher.publishEvent(BirthNumberAccessedEvent.viewed(viewedBy, memberId));
        }

        return member;
    }

    @Override
    public Member getMember(MemberId memberId) {
        return loadMember(memberId);
    }

    private Member loadMember(MemberId memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    /** Used when the finance module is not present in the Spring context (e.g., Modulith test slices). */
    private static class NoOpMemberFinancialStatePort implements MemberFinancialStatePort {
        @Override
        public MemberFinancialSnapshot getFinancialSnapshot(MemberId memberId) {
            return new MemberFinancialSnapshot(memberId, new MonetaryAmount(BigDecimal.ZERO, "CZK"), false);
        }
    }
}
