package com.klabis.common.users.infrastructure.jdbc;

import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

/**
 * Adapter that bridges between UserRepository domain interface and UserJdbcRepository.
 * <p>
 * This adapter implements:
 * <ul>
 *   <li>{@link UserRepository UserRepository} - domain repository interface</li>
 * </ul>
 * <p>
 * UserRepository extends {@link Users Users} public API, so this adapter indirectly implements both.
 * <p>
 * It handles conversion between User entities and UserMemento persistence objects.
 */
@SecondaryAdapter
@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJdbcRepository jdbcRepository;

    public UserRepositoryAdapter(UserJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public User save(User user) {
        // Convert User to UserMemento for persistence
        UserMemento saved = jdbcRepository.save(UserMemento.from(user));
        return saved.toUser();
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
    public void deleteById(UserId id) {
        jdbcRepository.deleteById(id.uuid());
    }

    @Override
    public boolean existsById(UserId id) {
        return jdbcRepository.existsById(id.uuid());
    }
}
