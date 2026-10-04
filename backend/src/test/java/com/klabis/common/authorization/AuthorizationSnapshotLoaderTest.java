package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.domain.UserPermissions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AuthorizationSnapshotLoader")
class AuthorizationSnapshotLoaderTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final RelationshipSource source = mock(RelationshipSource.class);
    private final UserId userId = UserId.newId();

    private AuthorizationSnapshotLoader loader() {
        var beans = new StaticListableBeanFactory(Map.of("source", source));
        return new AuthorizationSnapshotLoader(permissionService, beans.getBeanProvider(RelationshipSource.class));
    }

    @Test
    @DisplayName("loads nothing until asked")
    void shouldNotLoadOnConstruction() {
        loader();

        verifyNoInteractions(permissionService, source);
    }

    @Test
    @DisplayName("takes grants over everything from stored permissions and adds standard authorities")
    void shouldTakeGlobalAuthoritiesFromStoredPermissions() {
        when(permissionService.getUserPermissions(userId))
                .thenReturn(UserPermissions.create(userId, Set.of(Authority.MEMBERS_MANAGE)));
        when(source.grantsOf(userId)).thenReturn(Map.of());

        var snapshot = loader().loadFor(userId);

        assertThat(snapshot.overAll()).contains(Authority.MEMBERS_MANAGE)
                .containsAll(Authority.getStandardUserAuthorities())
                .doesNotContain(Authority.EVENTS_MANAGE);
    }

    @Test
    @DisplayName("discards relationship grants of an authority that may not be held over a target")
    void shouldDiscardGrantsOfAdministratorAuthorities() {
        TargetRef child = TargetRef.member(UUID.randomUUID());
        when(permissionService.getUserPermissions(userId)).thenReturn(UserPermissions.create(userId, Set.of()));
        when(source.grantsOf(userId)).thenReturn(Map.of(Authority.MEMBERS_MANAGE, Set.of(child)));

        var snapshot = loader().loadFor(userId);

        assertThat(snapshot.has(Authority.MEMBERS_MANAGE, child)).isFalse();
    }
}
