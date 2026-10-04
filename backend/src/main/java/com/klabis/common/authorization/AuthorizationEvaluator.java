package com.klabis.common.authorization;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.MethodSecurityAnnotations;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The single place that answers authorization questions, over the permission snapshot of the current request.
 */
@Component
public class AuthorizationEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationEvaluator.class);

    private final AuthorizationSnapshotProvider snapshots;
    private final ObjectProvider<ConversionService> conversionService;
    private final ConcurrentHashMap<InvocationKey, InvocationRules> rulesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Method, FieldRules> fieldRulesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Optional<RecordTarget>> recordTargetCache = new ConcurrentHashMap<>();

    public AuthorizationEvaluator(AuthorizationSnapshotProvider snapshots,
                                  ObjectProvider<ConversionService> conversionService) {
        this.snapshots = snapshots;
        this.conversionService = conversionService;
    }

    /**
     * Whether the annotations of {@code accessor} restrict who may see or change the field it exposes.
     */
    public static boolean isSecured(Method accessor) {
        return !FieldRules.of(accessor).isOpen();
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
        return target.type() == TargetType.MEMBER
                && SecurityContextHolder.getContext().getAuthentication() instanceof KlabisJwtAuthenticationToken token
                && token.isSelf(target.id());
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
     * Without an authenticated user the snapshot is empty and nothing is allowed.
     */
    public boolean canInvoke(Method method, Class<?> targetClass, Object @Nullable [] arguments) {
        InvocationRules rules = invocationRules(method, targetClass);
        if (rules.isOpen()) {
            return true;
        }
        return isAllowed(rules.authorities(), rules.targetOf(arguments, this::toUuid), rules.ownerVisible());
    }

    /**
     * The target an invocation of {@code method} with {@code arguments} is about, or {@code null} when the
     * method has no {@link TargetId} parameter or its argument is absent or not convertible to a UUID.
     */
    public @Nullable TargetRef targetOf(Method method, Class<?> targetClass, Object @Nullable [] arguments) {
        return invocationRules(method, targetClass).targetOf(arguments, this::toUuid);
    }

    /**
     * The parameter of {@code method} that identifies the target, for callers that read the target from the
     * request before the method is invoked (the request body is read before the arguments are resolved).
     */
    public @Nullable TargetParameter targetParameterOf(Method method, Class<?> targetClass) {
        InvocationRules rules = invocationRules(method, targetClass);
        return rules.targetType() != null && rules.targetIndex() >= 0
                ? new TargetParameter(rules.targetIndex(), rules.targetType())
                : null;
    }

    /**
     * Whether the current user may see the response field guarded by the annotations of {@code accessor}.
     * The record the field belongs to supplies the target: the component marked {@link TargetId} (or, for owner-visible
     * fields, the single component convertible to a UUID). A component holding several ids is a set of owners, not a
     * target: authorities must then be held over everything and the field is also visible to any of the owners.
     */
    public boolean canReadField(Method accessor, Object record) {
        FieldRules rules = fieldRules(accessor);
        if (rules.isOpen()) {
            return true;
        }
        return isAllowedOver(rules.authorities(), targetsOf(record), rules.ownerVisible());
    }

    /**
     * Whether the current user may change the request field guarded by the annotations of {@code accessor}
     * on the record identified by {@code target}.
     */
    public boolean canWriteField(Method accessor, @Nullable TargetRef target) {
        FieldRules rules = fieldRules(accessor);
        if (rules.isOpen()) {
            return true;
        }
        return isAllowedOver(rules.authorities(), targetList(target), rules.ownerVisible());
    }

    /**
     * Whether the current user may see the request field, either because they may change it or because they
     * hold one of its {@link ReadAuthority} authorities.
     */
    public boolean canReadRequestField(Method accessor, @Nullable TargetRef target) {
        if (canWriteField(accessor, target)) {
            return true;
        }
        FieldRules rules = fieldRules(accessor);
        if (rules.readAuthorities().isEmpty()) {
            return false;
        }
        return isAllowedOver(rules.readAuthorities(), targetList(target), false);
    }

    private boolean isAllowedOver(Collection<Authority> authorities, List<TargetRef> targets, boolean ownerVisible) {
        if (targets.size() == 1) {
            return isAllowed(authorities, targets.get(0), ownerVisible);
        }
        return authorities.stream().anyMatch(authority -> has(authority))
                || (ownerVisible && targets.stream().anyMatch(this::isSelf));
    }

    private static List<TargetRef> targetList(@Nullable TargetRef target) {
        return target != null ? List.of(target) : List.of();
    }

    private FieldRules fieldRules(Method accessor) {
        return fieldRulesCache.computeIfAbsent(accessor, FieldRules::of);
    }

    private InvocationRules invocationRules(Method method, Class<?> targetClass) {
        return rulesCache.computeIfAbsent(new InvocationKey(method, targetClass),
                key -> InvocationRules.of(method, targetClass));
    }

    private List<TargetRef> targetsOf(Object record) {
        RecordTarget recordTarget = recordTargetOf(record.getClass());
        if (recordTarget == null) {
            return List.of();
        }
        Object value;
        try {
            value = recordTarget.accessor().invoke(record);
        } catch (ReflectiveOperationException e) {
            LOG.warn("Failed to read target id from {}", record.getClass().getSimpleName(), e);
            return List.of();
        }
        Collection<?> values = value instanceof Collection<?> collection ? collection : value == null ? List.of() : List.of(value);
        return values.stream()
                .map(this::toUuid)
                .filter(Objects::nonNull)
                .map(id -> new TargetRef(recordTarget.type(), id))
                .toList();
    }

    private @Nullable RecordTarget recordTargetOf(Class<?> recordClass) {
        Optional<RecordTarget> cached = recordTargetCache.get(recordClass);
        if (cached != null) {
            return cached.orElse(null);
        }
        RecordComponent[] components = recordClass.getRecordComponents();
        if (components == null) {
            recordTargetCache.put(recordClass, Optional.empty());
            return null;
        }
        for (RecordComponent component : components) {
            Method accessor = component.getAccessor();
            TargetId targetId = accessor.getAnnotation(TargetId.class);
            if (targetId != null) {
                return cache(recordClass, new RecordTarget(accessor, targetId.value()));
            }
        }
        return discoverOwnerTarget(recordClass, components);
    }

    private @Nullable RecordTarget discoverOwnerTarget(Class<?> recordClass, RecordComponent[] components) {
        boolean anyOwnerVisible = Arrays.stream(components)
                .anyMatch(component -> component.getAccessor().getAnnotation(OwnerVisible.class) != null);
        if (!anyOwnerVisible) {
            recordTargetCache.put(recordClass, Optional.empty());
            return null;
        }
        ConversionService conversions = conversionService.getIfAvailable();
        if (conversions == null) {
            LOG.warn("Cannot resolve owner id of {}: ConversionService not available.", recordClass.getSimpleName());
            return null;
        }
        List<RecordComponent> candidates = Arrays.stream(components)
                .filter(component -> conversions.canConvert(component.getType(), UUID.class))
                .toList();
        if (candidates.size() == 1) {
            return cache(recordClass, new RecordTarget(candidates.get(0).getAccessor(), TargetType.MEMBER));
        }
        LOG.warn("Cannot resolve owner id of {}: found {} UUID-convertible components. Annotate the owner component with @TargetId.",
                recordClass.getSimpleName(), candidates.size());
        recordTargetCache.put(recordClass, Optional.empty());
        return null;
    }

    private RecordTarget cache(Class<?> recordClass, RecordTarget target) {
        target.accessor().trySetAccessible();
        recordTargetCache.put(recordClass, Optional.of(target));
        return target;
    }

    private record RecordTarget(Method accessor, TargetType type) {
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
            TargetId targetId = index >= 0
                    ? MethodSecurityAnnotations.findParameterAnnotation(method, targetClass, index, TargetId.class)
                    : null;
            return new InvocationRules(authorities, ownerVisible, index, targetId != null ? targetId.value() : null);
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
