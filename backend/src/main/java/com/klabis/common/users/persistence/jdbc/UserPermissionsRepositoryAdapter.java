package com.klabis.common.users.persistence.jdbc;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserPermissions;
import com.klabis.common.users.persistence.UserPermissionsRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

/**
 * Adapter that bridges between UserPermissionsRepository domain interface and UserPermissionsJdbcRepository.
 * <p>
 * This adapter implements the UserPermissionsRepository domain interface and delegates to UserPermissionsJdbcRepository.
 * It handles conversion between UserPermissions entities and UserPermissionsMemento persistence objects.
 * <p>
 * This is necessary to avoid signature clashes when extending both CrudRepository and domain interfaces.
 */
@SecondaryAdapter
@Repository
class UserPermissionsRepositoryAdapter implements UserPermissionsRepository {

    private final UserPermissionsJdbcRepository jdbcRepository;

    public UserPermissionsRepositoryAdapter(UserPermissionsJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public UserPermissions save(UserPermissions permissions) {
        // Check if this is a new or existing permissions record
        // This is necessary for UserPermissions created via create() which have isNew=true by default
        // but may already exist in database
        boolean exists = jdbcRepository.existsById(permissions.getUserId().uuid());

        // Convert UserPermissions to UserPermissionsMemento for persistence
        UserPermissionsMemento saved = jdbcRepository.save(UserPermissionsMemento.from(permissions, !exists));
        return saved.toUserPermissions();
    }

    @Override
    public Optional<UserPermissions> findById(UserId userId) {
        return jdbcRepository.findById(userId.uuid())
                .map(UserPermissionsMemento::toUserPermissions);
    }

    @Override
    public long countUsersWithAuthority(Authority authority) {
        return jdbcRepository.countActiveByAuthority(authority.getValue());
    }

    @Override
    public void deleteByUserId(UserId userId) {
        jdbcRepository.deleteById(userId.uuid());
    }
}
