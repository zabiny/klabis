package com.klabis.members.management;

import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import org.springframework.transaction.annotation.Transactional;

public interface ManagementService {

    @Transactional
    Member updateMember(MemberId memberId, Member.SelfUpdate command);

    @Transactional
    Member updateMember(MemberId memberId, Member.UpdateMemberByAdmin command);

    @Transactional
    Member terminateMember(MemberId memberId, Member.TerminateMembership command);

    @Transactional
    Member reactivateMember(MemberId memberId, Member.ReactivateMembership command);
}
