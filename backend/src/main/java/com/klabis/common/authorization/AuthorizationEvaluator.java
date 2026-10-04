package com.klabis.common.authorization;

import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.users.Authority;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * The single place that answers authorization questions, over the permission snapshot of the current request.
 * <p>
 * The {@link OwnershipResolver} is resolved on demand: it is a web-layer bean that cannot be injected eagerly.
 */
@Component
public class AuthorizationEvaluator {

    private final AuthorizationSnapshotProvider snapshots;
    private final ObjectProvider<OwnershipResolver> ownershipResolver;

    AuthorizationEvaluator(AuthorizationSnapshotProvider snapshots, ObjectProvider<OwnershipResolver> ownershipResolver) {
        this.snapshots = snapshots;
        this.ownershipResolver = ownershipResolver;
    }

    public boolean has(Authority authority) {
        return snapshots.current().hasOverAll(authority);
    }

    public boolean has(Authority authority, TargetRef target) {
        return snapshots.current().has(authority, target);
    }

    /**
     * Whether the target is the authenticated user themself. Only members are identified by a target
     * (a user's id is its member id).
     */
    public boolean isSelf(TargetRef target) {
        if (target.type() != TargetType.MEMBER) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OwnershipResolver resolver = ownershipResolver.getIfAvailable();
        return authentication != null && resolver != null && resolver.isOwner(target.id(), authentication);
    }

    /**
     * Rule for an operation or field guarded by {@code authorities} (any of them suffices) and optionally
     * visible to the owner: with a target an authority must be held over everything or over that target,
     * without a target over everything; an owner-visible element is also allowed when the target is the user.
     */
    public boolean isAllowed(Collection<Authority> authorities, @Nullable TargetRef target, boolean ownerVisible) {
        boolean authorityHeld = authorities.stream()
                .anyMatch(authority -> target != null ? has(authority, target) : has(authority));
        return authorityHeld || (ownerVisible && target != null && isSelf(target));
    }
}
