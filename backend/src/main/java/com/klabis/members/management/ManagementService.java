package com.klabis.members.management;

import com.klabis.members.domain.Member;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ManagementService {

    @Transactional
    Member updateMember(UUID memberId, Member.SelfUpdate command);

    @Transactional
    Member updateMember(UUID memberId, Member.UpdateMemberByAdmin command);

    @Transactional
    Member terminateMember(UUID memberId, Member.TerminateMembership command);
}
