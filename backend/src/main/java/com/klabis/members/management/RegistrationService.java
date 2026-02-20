package com.klabis.members.management;

import com.klabis.members.infrastructure.restapi.RegisterMemberRequest;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@PrimaryPort
public interface RegistrationService {
    /**
     * Registers a new member.
     * <p>
     * Creates both Member and User aggregates in an atomic transaction.
     * <b>Critical:</b> User is created FIRST to obtain the shared UserId,
     * then Member is created using that same ID. This ensures Member ID = User ID.
     * <p>
     * The MemberCreatedEvent will be published after commit, triggering
     * the password setup email to be sent asynchronously.
     *
     * @param request the registration request containing member details
     * @return the shared ID of the newly created member and user (same ID)
     * @throws IllegalArgumentException if any required field is invalid
     * @throws IllegalStateException    if Member ID != User ID after creation (invariant violation)
     */
    @Transactional
    UUID registerMember(RegisterMemberRequest request);
}
