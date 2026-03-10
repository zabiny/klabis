package com.klabis.common.users.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.common.users.domain.UserPermissionsRepository;
import com.klabis.common.users.domain.UserRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserPermissions JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class UserPermissionsRepositoryTest {

    @Autowired
    private UserPermissionsRepository permissionsRepository;

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String username) {
        return userRepository.save(User.createdUser(username, "$2a$10$hashvalue"));
    }

    @Nested
    @DisplayName("INSERT → READ → UPDATE cycle")
    class InsertReadUpdateCycle {

        @Test
        @DisplayName("should insert, read back, update and reflect updated authorities with non-null created_at")
        void shouldInsertReadAndUpdate() {
            // Given — a persisted user required by FK constraint
            User user = createTestUser("ZBM9101");
            UserId userId = user.getId();
            Set<Authority> initialAuthorities = Set.of(Authority.MEMBERS_READ);

            // INSERT
            UserPermissions inserted = permissionsRepository.save(
                    UserPermissions.create(userId, initialAuthorities));

            // READ
            Optional<UserPermissions> found = permissionsRepository.findById(userId);

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(userId);
            assertThat(found.get().getDirectAuthorities()).containsExactlyInAnyOrder(Authority.MEMBERS_READ);
            assertThat(found.get().getAuditMetadata()).isNotNull();
            assertThat(found.get().getAuditMetadata().createdAt()).isNotNull();

            // UPDATE — replace authorities
            UserPermissions toUpdate = found.get();
            toUpdate.replaceAuthorities(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
            UserPermissions updated = permissionsRepository.save(toUpdate);

            // Verify updated state
            Optional<UserPermissions> afterUpdate = permissionsRepository.findById(userId);

            assertThat(afterUpdate).isPresent();
            assertThat(afterUpdate.get().getDirectAuthorities())
                    .containsExactlyInAnyOrder(Authority.MEMBERS_READ, Authority.EVENTS_READ);
            assertThat(afterUpdate.get().getAuditMetadata()).isNotNull();
            assertThat(afterUpdate.get().getAuditMetadata().createdAt()).isNotNull();
        }
    }
}
