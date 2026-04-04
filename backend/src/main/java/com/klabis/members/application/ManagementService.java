package com.klabis.members.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.BirthNumberAccessedEvent;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagementService implements ManagementPort {

    private static final Logger log = LoggerFactory.getLogger(ManagementService.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final LastOwnershipChecker lastOwnershipChecker;
    private final ApplicationEventPublisher eventPublisher;

    public ManagementService(MemberRepository memberRepository, UserService userService,
                             LastOwnershipChecker lastOwnershipChecker, ApplicationEventPublisher eventPublisher) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.lastOwnershipChecker = lastOwnershipChecker;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public Member updateMember(MemberId memberId, Member.UpdateMember command) {
        Member member = loadMember(memberId);
        member.update(command);
        Member saved = memberRepository.save(member);
        log.info("Member updated: memberId={}", memberId);
        return saved;
    }

    @Transactional
    @Override
    public Member suspendMember(MemberId memberId, Member.SuspendMembership command) {
        Member member = loadMember(memberId);

        List<LastOwnershipChecker.OwnedGroupInfo> lastOwnedGroups = lastOwnershipChecker.findGroupsOwnedSolely(memberId);
        if (!lastOwnedGroups.isEmpty()) {
            throw new MemberIsLastGroupOwnerException(lastOwnedGroups);
        }

        log.info("Processing membership suspension: memberId={}, reason={}", memberId, command.reason());

        try {
            member.suspend(command);
        } catch (BusinessRuleViolationException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }

        Member saved = memberRepository.save(member);

        userService.suspendUser(member.getId().toUserId());

        log.info("Membership suspended: memberId={}, suspendedAt={}, suspendedBy={}",
                saved.getId(), saved.getSuspendedAt(), command.suspendedBy());

        return saved;
    }

    @Transactional
    @Override
    public Member resumeMember(MemberId memberId, Member.ResumeMembership command) {
        Member member = loadMember(memberId);

        log.info("Processing membership resume: memberId={}", memberId);

        try {
            member.resume(command);
        } catch (BusinessRuleViolationException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }

        Member saved = memberRepository.save(member);

        userService.resumeUser(member.getId().toUserId());

        log.info("Membership resumed: memberId={}, resumedBy={}", saved.getId(), command.resumedBy());

        return saved;
    }

    @Transactional
    @Override
    public Member getMemberAndRecordView(MemberId memberId, UserId viewedBy, boolean canManageMembers) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (!canManageMembers && !member.isActive()) {
            throw new MemberNotFoundException(memberId);
        }

        boolean isOwner = member.getUserId().equals(viewedBy);
        boolean canSeeBirthNumber = canManageMembers || isOwner;

        if (member.getBirthNumber() != null && canSeeBirthNumber) {
            eventPublisher.publishEvent(BirthNumberAccessedEvent.viewed(viewedBy, memberId));
        }

        return member;
    }

    private Member loadMember(MemberId memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }
}
