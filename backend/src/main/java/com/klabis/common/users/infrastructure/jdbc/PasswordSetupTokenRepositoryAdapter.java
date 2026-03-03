package com.klabis.common.users.infrastructure.jdbc;

import com.klabis.common.users.PasswordSetupTokenId;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.PasswordSetupToken;
import com.klabis.common.users.domain.TokenHash;
import com.klabis.common.users.infrastructure.PasswordSetupTokenRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter that bridges between PasswordSetupTokenRepository domain interface and PasswordSetupTokenJdbcRepository.
 * <p>
 * This adapter implements the PasswordSetupTokenRepository domain interface and delegates to PasswordSetupTokenJdbcRepository.
 * It handles conversion between PasswordSetupToken entities and PasswordSetupTokenMemento persistence objects.
 * <p>
 * This is necessary to avoid signature clashes when extending both CrudRepository and domain interfaces.
 */
@SecondaryAdapter
@Repository
class PasswordSetupTokenRepositoryAdapter implements PasswordSetupTokenRepository {

    private final PasswordSetupTokenJdbcRepository jdbcRepository;

    PasswordSetupTokenRepositoryAdapter(PasswordSetupTokenJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public PasswordSetupToken save(PasswordSetupToken token) {
        // Check if token already exists in database to determine INSERT vs UPDATE
        boolean exists = jdbcRepository.existsById(token.getId().uuid());

        // Convert PasswordSetupToken to PasswordSetupTokenMemento for persistence
        PasswordSetupTokenMemento memento = PasswordSetupTokenMemento.from(token);
        // Override isNew flag based on database existence
        memento.setNewFlag(!exists);

        PasswordSetupTokenMemento saved = jdbcRepository.save(memento);
        return saved.toPasswordSetupToken();
    }

    @Override
    public Optional<PasswordSetupToken> findByTokenHash(TokenHash tokenHash) {
        return jdbcRepository.findByTokenHash(tokenHash.getValue())
                .map(PasswordSetupTokenMemento::toPasswordSetupToken);
    }

    @Override
    public List<PasswordSetupToken> findActiveTokensForUser(UserId userId) {
        List<PasswordSetupTokenMemento> mementos =
                jdbcRepository.findActiveTokensForUser(userId.uuid(), Instant.now());
        return mementos.stream()
                .map(PasswordSetupTokenMemento::toPasswordSetupToken)
                .toList();
    }

    @Override
    public void invalidateAllForUser(UserId userId) {
        // Delete all tokens for this user (simpler than marking as expired)
        jdbcRepository.deleteAllByUserId(userId.uuid());
    }

    @Override
    public int deleteExpiredTokens() {
        return jdbcRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Override
    public Optional<PasswordSetupToken> findById(PasswordSetupTokenId id) {
        return jdbcRepository.findById(id.uuid())
                .map(PasswordSetupTokenMemento::toPasswordSetupToken);
    }

}
