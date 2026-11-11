package com.klabis.common.users.infrastructure.jdbc;

import com.klabis.common.users.domain.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for User aggregate using Memento pattern.
 * <p>
 * This repository manages {@link UserMemento} instances, which act as persistence adapters for the
 * pure domain {@link User} entity.
 * <p>
 * The memento pattern ensures:
 * - User entity remains a pure domain object without Spring annotations
 * - All JDBC persistence concerns are handled by UserMemento
 * - Domain events are still published via Spring Modulith's outbox pattern
 * <p>
 * Derived query methods:
 * - findByUsernameOrAltUsername: Find user memento by username or altUsername
 * - countActiveByAccountStatusAndAuthority: Custom SQL query for authority count
 */
@Repository
public interface UserJdbcRepository extends CrudRepository<UserMemento, UUID> {

    /**
     * Find user memento by username or altUsername. A username match wins over an altUsername match
     * of another user (no cross-column uniqueness is enforced).
     *
     * @param login the login to search for
     * @return optional containing user memento if found
     */
    @Query("""
            SELECT * FROM common.users WHERE user_name = :login OR alt_user_name = :login
            ORDER BY CASE WHEN user_name = :login THEN 0 ELSE 1 END
            LIMIT 1""")
    Optional<UserMemento> findByUsernameOrAltUsername(@Param("login") String login);
}
