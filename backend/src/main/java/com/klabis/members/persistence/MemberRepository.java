package com.klabis.members.persistence;

import com.klabis.members.Member;
import com.klabis.members.RegistrationNumber;
import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
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
 * Other modules should use {@link com.klabis.members.Members Members}
 * interface for querying members.
 */
@Repository
@SecondaryPort
public interface MemberRepository {

    /**
     * Saves a member to the repository.
     *
     * @param member the member to save
     * @return the saved member with generated ID
     */
    Member save(Member member);

    /**
     * Retrieves a member by their unique identifier.
     * <p>
     * Searches the repository for a member that matches the given UserId.
     *
     * @param memberId the unique identifier of the member
     * @return an {@code Optional} containing the member if found, or an empty {@code Optional} if no such member exists
     */
    Optional<Member> findById(UserId memberId);

    /**
     * Retrieves a member by their registration ID.
     * <p>
     * Searches the repository for a member associated with the given registration ID.
     *
     * @param registrationId the registration ID associated with the member
     * @return an {@code Optional} containing the member if found, or an empty {@code Optional} if no member exists with the given registration ID
     */
    Optional<Member> findByRegistrationId(RegistrationNumber registrationId);

    /**
     * Retrieves a member by their email address.
     * <p>
     * Searches the repository for a member with the given email address.
     * This is used for self-edit authorization (mapping OAuth2 subject to member).
     *
     * @param email the email address to search for
     * @return an {@code Optional} containing the member if found, or an empty {@code Optional} if no member exists with the given email
     */
    Optional<Member> findByEmail(String email);

    /**
     * Retrieves all members from the repository.
     * <p>
     * Returns all registered members without any specific ordering.
     * Note: This method returns unsorted results. Sorting can be added in future iterations if needed.
     *
     * @return a list of all members (empty list if no members exist)
     */
    List<Member> findAll();

    /**
     * Retrieves a page of members from the repository with pagination and sorting.
     * <p>
     * Supports paginated retrieval of members with configurable page size and sorting criteria.
     * The sorting can be applied to multiple fields (e.g., lastName, firstName).
     *
     * @param pageable the pagination and sorting parameters including page number, page size, and sort criteria
     * @return a page of members containing the requested page data, total element count, and page metadata
     */
    Page<Member> findAll(Pageable pageable);
}
