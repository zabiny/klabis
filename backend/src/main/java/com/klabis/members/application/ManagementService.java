package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ManagementService {

    Member updateMember(MemberId memberId, Member.UpdateMember command);

    Member suspendMember(MemberId memberId, Member.SuspendMembership command);

    Member resumeMember(MemberId memberId, Member.ResumeMembership command);

    /**
     * Loads a member and, when the member has a birth number AND the caller can see it,
     * publishes a VIEW_BIRTH_NUMBER audit event within the transaction so it is captured
     * by Spring Modulith's outbox.
     *
     * @param canManageMembers true when the caller holds MEMBERS_MANAGE authority
     */
    Member getMemberAndRecordView(MemberId memberId, UserId viewedBy, boolean canManageMembers);
}
