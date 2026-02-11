package com.klabis.users.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.users.Authority;
import com.klabis.users.UserId;
import com.klabis.users.UserPermissions;
import com.klabis.users.persistence.UserPermissionsRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter that bridges between UserPermissionsRepository domain interface and UserPermissionsJdbcRepository.
 * <p>
 * This adapter implements the UserPermissionsRepository domain interface and delegates to UserPermissionsJdbcRepository.
 * It handles conversion between UserPermissions entities and UserPermissionsMemento persistence objects.
 * <p>
 * This is necessary to avoid signature clashes when extending both CrudRepository and domain interfaces.
 */
@Repository
@Transactional
@SecondaryAdapter
public class UserPermissionsRepositoryAdapter implements UserPermissionsRepository {

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
        UserPermissionsMemento memento = UserPermissionsMemento.from(permissions, !exists);
        UserPermissionsMemento saved = jdbcRepository.save(memento);

        // Update audit metadata from saved memento
        AuditMetadata auditMetadata = saved.getAuditMetadata();
        if (auditMetadata != null) {
            permissions.updateAuditMetadata(auditMetadata);
        }

        // Mark as persisted
        permissions.markAsPersisted();

        // Return the same UserPermissions instance (now with updated audit metadata)
        return permissions;
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
