package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ManagementPort {

    /**
     * The baseline {@link Member.UpdateMember} for a member: every field pre-filled with the current
     * value. REST callers overlay only the fields their PATCH request changed, then pass the result
     * to {@link #updateMember(MemberId, Member.UpdateMember)}.
     *
     * @throws MemberNotFoundException if no member with the given id exists
     */
    Member.UpdateMember prefilledUpdateCommand(MemberId memberId);

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
