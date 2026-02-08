package com.klabis.users.persistence.jdbc;

import com.klabis.users.User;
import com.klabis.users.Users;
import com.klabis.users.Authority;
import com.klabis.users.UserAuditMetadata;
import com.klabis.users.UserId;
import com.klabis.users.persistence.UserRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter that bridges between Users public API, UserRepository domain interface and UserJdbcRepository.
 * <p>
 * This adapter implements both:
 * <ul>
 *   <li>{@link Users} - public API for other modules (read-only operations)</li>
 *   <li>{@link com.klabis.users.persistence.UserRepository UserRepository} - internal API for users module</li>
 * </ul>
 * <p>
 * It handles conversion between User entities and UserMemento persistence objects.
 */
@Component
@Transactional
@SecondaryAdapter
class UserRepositoryAdapter implements Users, UserRepository {

    private final UserJdbcRepository jdbcRepository;

    public UserRepositoryAdapter(UserJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public User save(User user) {
        // Convert User to UserMemento for persistence
        UserMemento memento = UserMemento.from(user);
        UserMemento saved = jdbcRepository.save(memento);

        // Update User's audit metadata from saved memento (if available)
        UserAuditMetadata auditMetadata = saved.getAuditMetadata();
        if (auditMetadata != null) {
            user.updateAuditMetadata(auditMetadata);
        }

        // Return the same User instance (now with updated audit metadata)
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jdbcRepository.findById(id.uuid())
                .map(UserMemento::toUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jdbcRepository.findByUsername(username)
                .map(UserMemento::toUser);
    }

    @Override
    public long countActiveUsersWithAuthority(Authority authority) {
        return jdbcRepository.countActiveByAccountStatusAndAuthority(authority.getValue());
    }

    @Override
    public void deleteById(UserId id) {
        jdbcRepository.deleteById(id.uuid());
    }

    @Override
    public boolean existsById(UserId id) {
        return jdbcRepository.existsById(id.uuid());
    }
}
