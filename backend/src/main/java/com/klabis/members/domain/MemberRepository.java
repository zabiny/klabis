package com.klabis.members.domain;

import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;


/**
 * Public query API for Member aggregate.
 * <p>
 * Provides read-only access to members for other modules.
 * This is the only public interface that should be used by external modules
 * to query member information.
 */
@Port
public interface MemberRepository {

    /**
     * Find a member by their unique ID.
     *
     * @param id the member's ID
     * @return optional containing the member if found
     */
    Optional<Member> findById(MemberId id);

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

    List<Member> findAll();

    List<Member> findAll(MemberFilter filter);

    Page<Member> findAll(MemberFilter filter, Pageable pageable);

    /**
     * Counts the number of members born in a specific year.
     * <p>
     * Used for registration number generation to determine the next sequence number.
     *
     * @param birthYear the birth year (e.g., 2005, 1995)
     * @return count of members born in that year
     */
    int countByBirthYear(int birthYear);

    boolean existsAny();

    Member save(Member member);
}
