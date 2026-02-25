package com.klabis.members.management;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for member update operations.
 *
 * <p>Authorization (self-edit check, field-level access control) is handled by the controller.
 * This service applies domain commands and persists the result.
 */
@Service
@PrimaryPort
class ManagementServiceImpl implements ManagementService {

    private static final Logger log = LoggerFactory.getLogger(ManagementServiceImpl.class);

    private final MemberRepository memberRepository;

    public ManagementServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    @Override
    public Member updateMember(UUID memberId, Member.SelfUpdate command) {
        Member member = loadMember(memberId);
        member.handle(command);
        Member saved = memberRepository.save(member);
        log.info("Member self-updated: memberId={}", memberId);
        return saved;
    }

    @Transactional
    @Override
    public Member updateMember(UUID memberId, Member.UpdateMemberByAdmin command) {
        Member member = loadMember(memberId);
        member.handle(command);
        Member saved = memberRepository.save(member);
        log.info("Member updated by admin: memberId={}", memberId);
        return saved;
    }

    @Transactional
    @Override
    public Member terminateMember(UUID memberId, UserId currentUserId, Member.TerminateMembership command) {
        Member member = loadMember(memberId);

        log.info("Processing membership termination: memberId={}, currentUserId={}, reason={}", memberId, currentUserId, command.reason());

        try {
            member.handle(command);
        } catch (BusinessRuleViolationException e) {
            throw new InvalidUpdateException(e.getMessage(), e);
        }

        Member saved = memberRepository.save(member);

        log.info("Membership terminated: memberId={}, terminatedAt={}, terminatedBy={}",
                saved.getId(), saved.getDeactivatedAt(), command.terminatedBy());

        return saved;
    }

    private Member loadMember(UUID memberId) {
        return memberRepository.findById(new UserId(memberId))
                .orElseThrow(() -> new InvalidUpdateException("Member not found with ID: " + memberId));
    }
}
