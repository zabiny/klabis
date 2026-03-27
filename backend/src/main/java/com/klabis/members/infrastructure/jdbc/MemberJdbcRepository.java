package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.domain.Member;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for Member aggregate using Memento pattern.
 * <p>
 * It manages {@link MemberMemento} instances, which act as persistence adapters for the
 * pure domain {@link Member} entity.
 * <p>
 * Note: This interface does NOT implement MemberRepository directly.
 * Instead, MemberRepositoryJdbcImpl wraps this repository and implements MemberRepository.
 * This is necessary because Spring Data JDBC repositories cannot extend custom interfaces
 * with different ID types (UserId vs UUID).
 * <p>
 * The memento pattern ensures:
 * - Member entity remains a pure domain object without Spring annotations
 * - All JDBC persistence concerns are handled by MemberMemento
 * - Domain events are still published via Spring Modulith's outbox pattern
 * <p>
 * Derived query methods:
 * - findByRegistrationNumber: Find member memento by registration number
 * - findByEmail: Find member memento by email address
 * - countByBirthYear: Count members born in a specific year (custom SQL query)
 * - findAllByActiveTrueOrderByLastNameAscFirstNameAsc: All active members, alphabetical order
 */
@Repository
interface MemberJdbcRepository extends CrudRepository<MemberMemento, UUID>, PagingAndSortingRepository<MemberMemento, UUID> {

    /**
     * Find member memento by registration number.
     *
     * @param registrationNumber the registration number to search for
     * @return Optional containing the member memento if found
     */
    Optional<MemberMemento> findByRegistrationNumber(String registrationNumber);

    /**
     * Find member memento by email address.
     *
     * @param email the email address to search for
     * @return Optional containing the member memento if found
     */
    Optional<MemberMemento> findByEmailEqualsIgnoreCase(String email);

    /**
     * Count members born in a specific year.
     * <p>
     * Uses custom SQL query to extract year from date_of_birth column.
     *
     * @param birthYear the birth year to count
     * @return number of members born in that year
     */
    @Query("SELECT COUNT(*) FROM members WHERE EXTRACT(YEAR FROM date_of_birth) = :birthYear")
    int countByBirthYear(@Param("birthYear") int birthYear);

    List<MemberMemento> findAllByActiveTrueOrderByLastNameAscFirstNameAsc();

    // findAll(Pageable) is inherited from PagingAndSortingRepository
    // findById(UUID) is inherited from CrudRepository
    // save() is inherited from CrudRepository
    // findAll() is inherited from CrudRepository
}
