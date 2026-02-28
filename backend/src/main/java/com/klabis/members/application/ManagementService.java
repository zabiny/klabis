package com.klabis.members.application;

import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import org.springframework.transaction.annotation.Transactional;

public interface ManagementService {

    @Transactional
    Member updateMember(MemberId memberId, Member.SelfUpdate command);

    @Transactional
    Member updateMember(MemberId memberId, Member.UpdateMemberByAdmin command);

    @Transactional
    Member suspendMember(MemberId memberId, Member.SuspendMembership command);

    @Transactional
    Member resumeMember(MemberId memberId, Member.ResumeMembership command);
}
