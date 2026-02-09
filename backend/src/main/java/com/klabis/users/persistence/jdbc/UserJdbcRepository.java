package com.klabis.users.persistence.jdbc;

import com.klabis.users.User;
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
 * - findByUsername: Find user memento by username (registration number)
 * - countActiveByAccountStatusAndAuthority: Custom SQL query for authority count
 */
@Repository
public interface UserJdbcRepository extends CrudRepository<UserMemento, UUID> {

    /**
     * Find user memento by username (registration number).
     * <p>
     * Derived query - Spring Data JDBC generates SQL automatically.
     *
     * @param username the username to search for
     * @return optional containing user memento if found
     */
    Optional<UserMemento> findByUsername(String username);
}
