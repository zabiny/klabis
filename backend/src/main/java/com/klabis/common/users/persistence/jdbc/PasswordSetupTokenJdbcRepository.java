package com.klabis.common.users.persistence.jdbc;

import com.klabis.common.users.PasswordSetupToken;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for PasswordSetupToken aggregate using Memento pattern.
 * <p>
 * This repository manages {@link PasswordSetupTokenMemento} instances, which act as persistence adapters for the
 * pure domain {@link PasswordSetupToken} entity.
 * <p>
 * The memento pattern ensures:
 * - PasswordSetupToken entity remains a pure domain object without Spring annotations
 * - All JDBC persistence concerns are handled by PasswordSetupTokenMemento
 * <p>
 * Derived and custom query methods:
 * - findByTokenHash: Find token memento by token hash
 * - findActiveTokensForUser: Custom SQL query to find active (unused and not expired) tokens for a user
 * - deleteByExpiresAtBefore: Modifying query to delete expired tokens
 * - deleteAllByUserId: Modifying query to delete all tokens for a user
 */
public interface PasswordSetupTokenJdbcRepository extends CrudRepository<PasswordSetupTokenMemento, UUID> {

    /**
     * Find token memento by token hash.
     * <p>
     * Derived query - Spring Data JDBC generates SQL automatically.
     *
     * @param tokenHash the token hash to search for
     * @return optional containing token memento if found
     */
    Optional<PasswordSetupTokenMemento> findByTokenHash(String tokenHash);

    /**
     * Find all active tokens for a user.
     * <p>
     * Active tokens are those that:
     * - Belong to the specified user
     * - Have not been used (used_at IS NULL)
     * - Have not expired (expires_at > :currentTime)
     * <p>
     * Uses custom SQL query to check both conditions.
     *
     * @param userId      the user ID
     * @param currentTime the current time to check expiration against
     * @return list of active token mementos for the user
     */
    @Query("""
            SELECT *
            FROM password_setup_tokens
            WHERE user_id = :userId
            AND used_at IS NULL
            AND expires_at > :currentTime
            ORDER BY created_at DESC
            """)
    List<PasswordSetupTokenMemento> findActiveTokensForUser(@Param("userId") UUID userId, @Param("currentTime") Instant currentTime);

    /**
     * Delete all tokens that have expired.
     * <p>
     * Uses modifying query to delete tokens where expires_at < :expirationTime.
     * This is called by the scheduled cleanup job.
     *
     * @param expirationTime the cutoff time for expiration
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("""
            DELETE FROM password_setup_tokens
            WHERE expires_at < :expirationTime
            """)
    int deleteByExpiresAtBefore(@Param("expirationTime") Instant expirationTime);

    /**
     * Delete all tokens for a specific user.
     * <p>
     * Uses modifying query to delete all tokens associated with the user ID.
     * This is called when invalidating all tokens for a user (e.g., when generating a new token).
     *
     * @param userId the user ID whose tokens should be deleted
     */
    @Modifying
    @Query("""
            DELETE FROM password_setup_tokens
            WHERE user_id = :userId
            """)
    void deleteAllByUserId(@Param("userId") UUID userId);
}
