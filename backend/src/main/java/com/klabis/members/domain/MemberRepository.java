package com.klabis.members.domain;

import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Internal repository interface for Member aggregate.
 * <p>
 * Defines persistence operations for the Member bounded context.
 * Implementation will be provided in the infrastructure layer.
 *
 * @apiNote This is internal API for use within the members module only.
 * Other modules should use {@link Members Members}
 * interface for querying members.
 */
@SecondaryPort
public interface MemberRepository extends Members {

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

    /**
     * Counts the number of members born in a specific year.
     * <p>
     * Used for registration number generation to determine the next sequence number.
     *
     * @param birthYear the birth year (e.g., 2005, 1995)
     * @return count of members born in that year
     */
    int countByBirthYear(int birthYear);

    /**
     * Saves a member to the repository.
     *
     * @param member the member to save
     * @return the saved member with generated ID
     */
    Member save(Member member);

    /**
     * Retrieves all members from the repository.
     * <p>
     * Returns all registered members without any specific ordering.
     * Note: This method returns unsorted results. Sorting can be added in future iterations if needed.
     *
     * @return a list of all members (empty list if no members exist)
     */
    List<Member> findAll();

}
