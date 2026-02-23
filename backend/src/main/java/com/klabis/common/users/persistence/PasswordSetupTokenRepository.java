package com.klabis.common.users.persistence;

import com.klabis.common.users.PasswordSetupToken;
import com.klabis.common.users.TokenHash;
import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link PasswordSetupToken} aggregate.
 *
 * <p>Provides persistence operations for password setup tokens.
 */
@SecondaryPort
public interface PasswordSetupTokenRepository {

    /**
     * Saves a password setup token.
     *
     * @param token the token to save
     * @return the saved token
     */
    PasswordSetupToken save(PasswordSetupToken token);

    /**
     * Finds a token by its hash.
     *
     * @param tokenHash the token hash to search for
     * @return optional containing the token, or empty if not found
     */
    Optional<PasswordSetupToken> findByTokenHash(TokenHash tokenHash);

    /**
     * Finds all active (unused and not expired) tokens for a user.
     *
     * @param userId the user ID
     * @return list of active tokens for the user
     */
    List<PasswordSetupToken> findActiveTokensForUser(UserId userId);

    /**
     * Invalidates all tokens for a user by marking them as expired.
     *
     * <p>This is used when generating a new token for a user
     * to ensure only one valid token exists at a time.
     *
     * @param userId the user ID
     */
    void invalidateAllForUser(UserId userId);

    /**
     * Deletes all expired tokens from the database.
     *
     * <p>This is called by the scheduled cleanup job to remove old tokens.
     *
     * @return the number of tokens deleted
     */
    int deleteExpiredTokens();

    /**
     * Finds a token by its ID.
     *
     * @param id the token ID
     * @return optional containing the token, or empty if not found
     */
    Optional<PasswordSetupToken> findById(UUID id);

    /**
     * Finds all tokens (for testing purposes).
     *
     * @return list of all tokens
     */
    List<PasswordSetupToken> findAll();
}
