package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the complete snapshot of a user: grants over everything from {@link PermissionService} and the united
 * grants of all {@link RelationshipSource} beans. Called at most once per request, by the authentication token.
 */
@Component
public class AuthorizationSnapshotLoader {

    private final PermissionService permissionService;
    private final ObjectProvider<RelationshipSource> relationshipSources;

    AuthorizationSnapshotLoader(PermissionService permissionService,
                                ObjectProvider<RelationshipSource> relationshipSources) {
        this.permissionService = permissionService;
        this.relationshipSources = relationshipSources;
    }

    public AuthorizationSnapshot loadFor(UserId userId) {
        Set<Authority> overAll = Authority.withStandard(permissionService.getUserPermissions(userId).getDirectAuthorities());
        List<Map<Authority, Set<TargetRef>>> sourceGrants = relationshipSources.orderedStream()
                .map(source -> source.grantsOf(userId))
                .toList();
        return AuthorizationSnapshot.fromSources(overAll, sourceGrants);
    }
}
