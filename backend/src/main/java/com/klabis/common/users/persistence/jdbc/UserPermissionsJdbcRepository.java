package com.klabis.common.users.persistence.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JDBC repository for UserPermissions aggregate using Memento pattern.
 * <p>
 * This repository manages {@link UserPermissionsMemento} instances, which act as persistence adapters for the
 * pure domain {@link com.klabis.common.users.UserPermissions} entity.
 * <p>
 * The memento pattern ensures:
 * - UserPermissions entity remains a pure domain object without Spring annotations
 * - All JDBC persistence concerns are handled by UserPermissionsMemento
 * <p>
 * Queries:
 * - countActiveByAuthority: Custom SQL query for counting active users with specific authority
 */
@Repository
public interface UserPermissionsJdbcRepository extends CrudRepository<UserPermissionsMemento, UUID> {

    /**
     * Count active users with a specific direct authority.
     * <p>
     * Uses custom SQL query to:
     * - Join user_permissions with users table
     * - Search for authority string in JSON array using LIKE
     * - Filter by ACTIVE account status
     * <p>
     * Note: This only counts direct authorities (not group-based, since groups don't exist yet).
     * Uses LIKE search for JSON array which works reliably across H2 and PostgreSQL.
     *
     * @param authority the authority to check for (JSON string value)
     * @return count of active users with the specified direct authority
     */
    @Query("""
            SELECT COUNT(*)
            FROM user_permissions up
                inner join users u on up.user_id = u.id
            WHERE u.account_status = 'ACTIVE'
            AND up.authorities LIKE '%' || :authority || '%'
            """)
    long countActiveByAuthority(@Param("authority") String authority);
}
