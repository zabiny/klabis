package com.klabis.members;

import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Public query API for Member aggregate.
 * <p>
 * Provides read-only access to members for other modules.
 * This is the only public interface that should be used by external modules
 * to query member information.
 */
@Port
public interface Members {

    /**
     * Find a member by their unique ID.
     *
     * @param id the member's user ID
     * @return optional containing the member if found
     */
    Optional<Member> findById(UserId id);

    /**
     * Find a member by their registration number.
     *
     * @param registrationNumber the registration number
     * @return optional containing the member if found
     */
    Optional<Member> findByRegistrationNumber(RegistrationNumber registrationNumber);

    /**
     * Find a member by their email address.
     *
     * @param email the email address
     * @return optional containing the member if found
     */
    Optional<Member> findByEmail(String email);

    /**
     * Find all members with pagination.
     *
     * @param pageable pagination parameters
     * @return page of members
     */
    Page<Member> findAll(Pageable pageable);
}
