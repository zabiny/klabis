package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    /**
     * Applies an inward write from ORIS synchronisation to an existing member.
     *
     * @throws MemberNotFoundException if no member with the given id exists
     */
    Member syncMemberFromOris(MemberId memberId, Member.SyncFromOris command);

    Member suspendMember(MemberId memberId, Member.SuspendMembership command);

    Member resumeMember(MemberId memberId, Member.ResumeMembership command);

    /**
     * Loads a member and, when the member has a birth number AND the caller can see it,
     * publishes a VIEW_BIRTH_NUMBER audit event within the transaction so it is captured
     * by Spring Modulith's outbox.
     *
     * @param access what the caller may see, as decided by the authorization rules
     * @throws MemberNotFoundException if the member does not exist, or is suspended and {@code access} hides suspended members
     */
    Member getMemberAndRecordView(MemberId memberId, UserId viewedBy, MemberViewAccess access);

    /**
     * Plain read with no audit-event side effect — for system-triggered reads (e.g.
     * synchronisation) that have no acting user to attribute a birth-number access to.
     *
     * @throws MemberNotFoundException if no member with the given id exists
     */
    Member getMember(MemberId memberId);

    /**
     * Performs no authorization: the caller must restrict the filter (e.g. status, incompleteness) to what the user may see.
     */
    Page<Member> listMembers(MemberFilter filter, Pageable pageable);

    List<Member> listActiveMembers();
}
