package com.klabis.members.management;

import com.klabis.members.domain.Member;
import com.klabis.members.infrastructure.restapi.UpdateMemberRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ManagementService {
    /**
     * Updates a member's information.
     * <p>
     * Implements dual authorization:
     * <ol>
     *   <li>Determine if user is admin (has MEMBERS:UPDATE)</li>
     *   <li>If not admin, verify self-edit (authenticated registration number matches member)</li>
     *   <li>Filter fields based on role (non-admins cannot update admin-only fields)</li>
     *   <li>Apply updates using Member's update methods</li>
     *   <li>Save updated member to repository</li>
     * </ol>
     *
     * @param memberId the ID of the member to update
     * @param request  the update request
     * @return the ID of the updated member
     * @throws SelfEditNotAllowedException if non-admin tries to edit another member
     * @throws AdminFieldAccessException   if non-admin tries to edit admin-only fields
     * @throws UserIdentificationException if user cannot be identified from authentication
     * @throws InvalidUpdateException      if validation fails
     */
    @Transactional
    UUID updateMember(UUID memberId, UpdateMemberRequest request);

    /**
     * Terminates a member's membership.
     * <p>
     * This method handles the complete termination workflow:
     * <ol>
     *   <li>Verifies user has MEMBERS:UPDATE authority (admin-only operation)</li>
     *   <li>Loads the member from repository</li>
     *   <li>Invokes domain command to terminate membership</li>
     *   <li>Publishes domain event (MemberTerminatedEvent)</li>
     *   <li>Saves updated member to repository</li>
     * </ol>
     *
     * @param memberId the ID of the member to terminate
     * @param command  the termination command containing terminatedBy, reason and optional note
     * @return the ID of the terminated member
     * @throws InvalidUpdateException                                    if user lacks MEMBERS:UPDATE permission
     * @throws InvalidUpdateException                                    if member not found
     * @throws org.springframework.dao.OptimisticLockingFailureException if concurrent modification detected
     */
    @Transactional
    UUID terminateMember(UUID memberId, Member.TerminateMembership command);
}
