package com.klabis.common.authorization;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The single place that answers authorization questions, over the permission snapshot of the current request.
 */
@Component
public class AuthorizationEvaluator {

    private final AuthorizationSnapshotProvider snapshots;
    private final UuidConversion uuids;
    private final RecordTargets recordTargets;
    private final ConcurrentHashMap<InvocationKey, InvocationRules> invocationRulesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Method, FieldRules> fieldRulesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<PropertyKey, Optional<Method>> securedAccessorCache = new ConcurrentHashMap<>();

    public AuthorizationEvaluator(AuthorizationSnapshotProvider snapshots,
                                  ObjectProvider<ConversionService> conversionService) {
        this.snapshots = snapshots;
        this.uuids = new UuidConversion(conversionService);
        this.recordTargets = new RecordTargets(uuids);
    }

    /**
     * Whether the annotations of {@code accessor} restrict who may see or change the field it exposes.
     */
    public static boolean isSecured(Method accessor) {
        return !FieldRules.of(accessor).isOpen();
    }

    /**
     * Whether {@code method} carries any rule {@link #canInvoke} would enforce, so the method needs guarding.
     */
    public static boolean isGuarded(Method method, Class<?> targetClass) {
        return !InvocationRules.of(method, targetClass).isOpen();
    }

    public boolean has(Authority authority) {
        return snapshots.current().hasOverAll(authority);
    }

    public boolean has(Authority authority, TargetRef target) {
        return snapshots.current().has(authority, target);
    }

    /**
     * Ids of the targets of {@code type} over which the user holds {@code authority} through a grant over
     * specific targets. Lets list queries be restricted to granted targets; grants over everything are
     * answered by {@link #has(Authority)}.
     */
    public Set<UUID> targetIdsOf(Authority authority, TargetType type) {
        return snapshots.current().targetsOf(authority).stream()
                .filter(target -> target.type() == type)
                .map(TargetRef::id)
                .collect(Collectors.toUnmodifiableSet());
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
        return isAllowedOver(snapshots.current(), authorities, () -> targetList(target), ownerVisible);
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
        return isAllowedOver(snapshots.current(), rules.authorities(),
                () -> targetsOf(rules, arguments), rules.ownerVisible());
    }

    /**
     * The target of kind {@code kind} an invocation of {@code method} with {@code arguments} is about, or
     * {@code null} when the method has no {@link TargetId} parameter of that kind or its argument is absent or
     * not convertible to a UUID.
     */
    public @Nullable TargetRef targetOf(Method method, Class<?> targetClass, Object @Nullable [] arguments,
                                        TargetType kind) {
        return targetOf(invocationRules(method, targetClass), arguments, kind);
    }

    /**
     * The parameter of {@code method} that identifies the target of kind {@code kind}, for callers that read the
     * target from the request before the method is invoked (the request body is read before the arguments are
     * resolved).
     */
    public @Nullable TargetParameter targetParameterOf(Method method, Class<?> targetClass, TargetType kind) {
        return invocationRules(method, targetClass).targetOfKind(kind);
    }

    /**
     * The target of the given type identified by {@code id}, converted to a UUID the same way as the arguments
     * of {@link #canInvoke}; {@code null} when it is absent or not convertible.
     */
    public @Nullable TargetRef toTarget(TargetType type, @Nullable Object id) {
        UUID uuid = uuids.toUuid(id);
        return uuid != null ? new TargetRef(type, uuid) : null;
    }

    /**
     * Whether the current user may see the response field guarded by the annotations of {@code accessor}.
     * The record the field belongs to supplies the target: the component marked {@link TargetId} (or, for owner-visible
     * fields, the single component convertible to a UUID). A component holding several ids is a set of owners, not a
     * target: authorities must then be held over everything and the field is also visible to any of the owners.
     */
    public boolean canReadField(Method accessor, Object record) {
        return isFieldAllowed(accessor, () -> recordTargets.of(record));
    }

    /**
     * Whether the current user may see the property {@code property} of the response record {@code recordType}
     * when the record is about {@code target}, by the same rule {@link #canReadField(Method, Object)} applies to the
     * serialized field. For callers that must decide before the response record exists. A property without
     * security annotations is not restricted; a property the record does not have is a programming error and
     * fails loudly rather than silently reporting the field as visible.
     *
     * @throws IllegalArgumentException when {@code recordType} has no property named {@code property}
     */
    public boolean canReadField(Class<?> recordType, String property, @Nullable TargetRef target) {
        Method accessor = FieldRules.responseAccessor(recordType, property);
        return accessor == null || isFieldAllowed(accessor, () -> targetList(target));
    }

    /**
     * Whether the current user may change the request field guarded by the annotations of {@code accessor}
     * on the record identified by {@code target}.
     */
    public boolean canWriteField(Method accessor, Function<TargetType, @Nullable TargetRef> targetOfKind) {
        FieldRules rules = fieldRules(accessor);
        return isFieldAllowed(accessor, () -> targetsOfKinds(rules, targetOfKind));
    }

    private boolean isFieldAllowed(Method accessor, Supplier<List<TargetRef>> targets) {
        FieldRules rules = fieldRules(accessor);
        if (rules.isOpen()) {
            return true;
        }
        return isAllowedOver(snapshots.current(), rules.authorities(), targets, rules.ownerVisible());
    }

    /**
     * What the current user may do with the property {@code property} of the request payload {@code payloadType}
     * on the record identified by {@code target}: change it, see it (because they hold one of its
     * {@link com.klabis.common.security.fieldsecurity.ReadAuthority} authorities) or neither. A property
     * without security annotations is not restricted.
     */
    public FieldAccess requestFieldAccess(@Nullable Class<?> payloadType, String property,
                                          Function<TargetType, @Nullable TargetRef> targetOfKind) {
        Method accessor = securedAccessor(payloadType, property);
        if (accessor == null) {
            return FieldAccess.WRITE;
        }
        FieldRules rules = fieldRules(accessor);
        AuthorizationSnapshot snapshot = snapshots.current();
        Supplier<List<TargetRef>> targets = () -> targetsOfKinds(rules, targetOfKind);
        if (isAllowedOver(snapshot, rules.authorities(), targets, rules.ownerVisible())) {
            return FieldAccess.WRITE;
        }
        if (!rules.readAuthorities().isEmpty() && isAllowedOver(snapshot, rules.readAuthorities(), targets, false)) {
            return FieldAccess.READ;
        }
        return FieldAccess.NONE;
    }

    /**
     * Text describing what {@link #canInvoke} requires of the user for {@code method}, for error messages.
     */
    public String describeRequirement(Method method, Class<?> targetClass) {
        return invocationRules(method, targetClass).describe();
    }

    /**
     * Text describing what the user must hold to see or change the field guarded by {@code accessor}.
     */
    public String describeRequirement(Method accessor) {
        return fieldRules(accessor).describe();
    }

    private boolean isAllowedOver(AuthorizationSnapshot snapshot, Collection<Authority> authorities,
                                  Supplier<List<TargetRef>> targets, boolean ownerVisible) {
        if (authorities.stream().anyMatch(snapshot::hasOverAll)) {
            return true;
        }
        boolean anyTargetedGrant = authorities.stream().anyMatch(authority -> !snapshot.targetsOf(authority).isEmpty());
        if (!ownerVisible && !anyTargetedGrant) {
            return false;
        }
        List<TargetRef> candidates = targets.get();
        return authorities.stream().anyMatch(authority -> heldOverSoleTargetOfOwnKind(snapshot, authority, candidates))
                || (ownerVisible && candidates.stream().anyMatch(this::isSelf));
    }

    /**
     * Several targets of one kind come from a component holding several ids (a set of owners), which is not a
     * target an authority can be held over.
     */
    private static boolean heldOverSoleTargetOfOwnKind(AuthorizationSnapshot snapshot, Authority authority,
                                                       List<TargetRef> candidates) {
        List<TargetRef> ofKind = candidates.stream()
                .filter(target -> target.type() == authority.getTargetType())
                .toList();
        return ofKind.size() == 1 && snapshot.has(authority, ofKind.get(0));
    }

    private List<TargetRef> targetsOf(InvocationRules rules, Object @Nullable [] arguments) {
        return rules.targets().keySet().stream()
                .map(kind -> targetOf(rules, arguments, kind))
                .filter(Objects::nonNull)
                .toList();
    }

    private @Nullable TargetRef targetOf(InvocationRules rules, Object @Nullable [] arguments, TargetType kind) {
        TargetParameter parameter = rules.targetOfKind(kind);
        if (parameter == null || arguments == null || parameter.index() >= arguments.length) {
            return null;
        }
        return toTarget(parameter.type(), arguments[parameter.index()]);
    }

    private static List<TargetRef> targetsOfKinds(FieldRules rules, Function<TargetType, @Nullable TargetRef> targetOfKind) {
        return rules.targetKinds().stream()
                .map(targetOfKind)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<TargetRef> targetList(@Nullable TargetRef target) {
        return target != null ? List.of(target) : List.of();
    }

    private @Nullable Method securedAccessor(@Nullable Class<?> payloadType, String property) {
        return securedAccessorCache
                .computeIfAbsent(new PropertyKey(payloadType, property),
                        key -> Optional.ofNullable(FieldRules.securedAccessor(payloadType, property)))
                .orElse(null);
    }

    private FieldRules fieldRules(Method accessor) {
        return fieldRulesCache.computeIfAbsent(accessor, FieldRules::of);
    }

    private InvocationRules invocationRules(Method method, Class<?> targetClass) {
        return invocationRulesCache.computeIfAbsent(new InvocationKey(method, targetClass),
                key -> InvocationRules.of(method, targetClass));
    }

    private record InvocationKey(Method method, Class<?> targetClass) {
    }

    private record PropertyKey(@Nullable Class<?> payloadType, String property) {
    }
}
