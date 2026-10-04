package com.klabis.common.authorization;

import com.klabis.common.security.MethodSecurityAnnotations;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The single place that answers authorization questions, over the permission snapshot of the current request.
 * <p>
 * The {@link OwnershipResolver} is resolved on demand: it is a web-layer bean that cannot be injected eagerly.
 */
@Component
public class AuthorizationEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationEvaluator.class);

    private final AuthorizationSnapshotProvider snapshots;
    private final ObjectProvider<OwnershipResolver> ownershipResolver;
    private final ObjectProvider<ConversionService> conversionService;
    private final ConcurrentHashMap<InvocationKey, InvocationRules> rulesCache = new ConcurrentHashMap<>();

    public AuthorizationEvaluator(AuthorizationSnapshotProvider snapshots,
                           ObjectProvider<OwnershipResolver> ownershipResolver,
                           ObjectProvider<ConversionService> conversionService) {
        this.snapshots = snapshots;
        this.ownershipResolver = ownershipResolver;
        this.conversionService = conversionService;
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

    /**
     * Whether the current user may invoke {@code method} with {@code arguments}, according to the
     * {@link HasAuthority}, {@link OwnerVisible} and {@link TargetId} annotations of the method (and of its class,
     * for the authorities). A method without any of them is open. The target is the argument of the parameter
     * annotated with {@link TargetId}; an absent or non-convertible value means the invocation has no target.
     * <p>
     * Outside of an HTTP request there is no permission snapshot and nothing is allowed.
     */
    public boolean canInvoke(Method method, Class<?> targetClass, Object @Nullable [] arguments) {
        InvocationRules rules = rulesCache.computeIfAbsent(new InvocationKey(method, targetClass),
                key -> InvocationRules.of(method, targetClass));
        if (rules.isOpen()) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        try {
            return isAllowed(rules.authorities(), rules.targetOf(arguments, this::toUuid), rules.ownerVisible());
        } catch (ScopeNotActiveException e) {
            LOG.warn("Authorization of {} requested outside of an HTTP request, denying", method);
            return false;
        }
    }

    private @Nullable UUID toUuid(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        ConversionService conversions = conversionService.getIfAvailable();
        if (conversions == null || !conversions.canConvert(value.getClass(), UUID.class)) {
            return null;
        }
        return conversions.convert(value, UUID.class);
    }

    private record InvocationKey(Method method, Class<?> targetClass) {
    }

    private record InvocationRules(List<Authority> authorities, boolean ownerVisible, int targetIndex,
                                   @Nullable TargetType targetType) {

        static InvocationRules of(Method method, Class<?> targetClass) {
            HasAuthority hasAuthority = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HasAuthority.class);
            if (hasAuthority == null) {
                hasAuthority = MethodSecurityAnnotations.findClassAnnotation(targetClass, HasAuthority.class);
            }
            List<Authority> authorities = hasAuthority != null ? List.of(hasAuthority.value()) : List.of();
            boolean ownerVisible = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, OwnerVisible.class) != null;

            int index = MethodSecurityAnnotations.findAnnotatedParameterIndex(method, targetClass, TargetId.class);
            if (index >= 0) {
                TargetId targetId = MethodSecurityAnnotations.findParameterAnnotation(method, targetClass, index, TargetId.class);
                return new InvocationRules(authorities, ownerVisible, index, targetId != null ? targetId.value() : null);
            }
            int ownerIdIndex = MethodSecurityAnnotations.findAnnotatedParameterIndex(method, targetClass, OwnerId.class);
            return new InvocationRules(authorities, ownerVisible, ownerIdIndex,
                    ownerIdIndex >= 0 ? TargetType.MEMBER : null);
        }

        boolean isOpen() {
            return authorities.isEmpty() && !ownerVisible;
        }

        @Nullable TargetRef targetOf(Object @Nullable [] arguments, Function<Object, @Nullable UUID> toUuid) {
            if (targetType == null || arguments == null || targetIndex >= arguments.length) {
                return null;
            }
            UUID id = toUuid.apply(arguments[targetIndex]);
            return id != null ? new TargetRef(targetType, id) : null;
        }
    }
}
