package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Immutable set of grants of one user, used for every authorization decision within a request.
 * <p>
 * An authority is held either over everything of its target type (including targets created later) or over
 * specific targets.
 */
public final class AuthorizationSnapshot {

    private static final AuthorizationSnapshot EMPTY = new AuthorizationSnapshot(Set.of(), Map.of());

    private final Set<Authority> overAll;
    private final Map<Authority, Set<TargetRef>> overTargets;

    private AuthorizationSnapshot(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        this.overAll = Set.copyOf(overAll);
        Map<Authority, Set<TargetRef>> copy = new EnumMap<>(Authority.class);
        overTargets.forEach((authority, targets) -> {
            if (!targets.isEmpty()) {
                copy.put(authority, Set.copyOf(targets));
            }
        });
        this.overTargets = Map.copyOf(copy);
    }

    public static AuthorizationSnapshot empty() {
        return EMPTY;
    }

    /**
     * Builds a snapshot from grants taken as given, for tests. Production snapshots go through
     * {@link #fromSources(Set, Collection)}, which discards the grants a source is not entitled to hand out.
     */
    static AuthorizationSnapshot of(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        return new AuthorizationSnapshot(overAll, overTargets);
    }

    /**
     * Snapshot of an authentication that carries its authorities itself (machine-to-machine tokens): the known
     * authorities are held over everything, there are no targeted grants.
     */
    public static AuthorizationSnapshot ofGrantedAuthorities(Collection<? extends GrantedAuthority> granted) {
        Set<Authority> known = EnumSet.noneOf(Authority.class);
        for (GrantedAuthority authority : granted) {
            if (Authority.isKnownAuthority(authority.getAuthority())) {
                known.add(Authority.fromString(authority.getAuthority()));
            }
        }
        return new AuthorizationSnapshot(known, Map.of());
    }

    public static AuthorizationSnapshot fromSources(Set<Authority> overAll,
                                                    Collection<Map<Authority, Set<TargetRef>>> sourceGrants) {
        return fromSources(overAll, sourceGrants, Authority.delegatable()::contains);
    }

    static AuthorizationSnapshot fromSources(Set<Authority> overAll,
                                             Collection<Map<Authority, Set<TargetRef>>> sourceGrants,
                                             Predicate<Authority> holdableOverTargets) {
        Map<Authority, Set<TargetRef>> united = new EnumMap<>(Authority.class);
        for (Map<Authority, Set<TargetRef>> grants : sourceGrants) {
            grants.forEach((authority, targets) -> {
                if (!holdableOverTargets.test(authority)) {
                    return;
                }
                targets.stream()
                        .filter(target -> target.type() == authority.getTargetType())
                        .forEach(target -> united.computeIfAbsent(authority, a -> new HashSet<>()).add(target));
            });
        }
        return new AuthorizationSnapshot(overAll, united);
    }

    public Set<Authority> overAll() {
        return overAll;
    }

    public boolean hasOverAll(Authority authority) {
        return overAll.contains(authority);
    }

    public boolean has(Authority authority, TargetRef target) {
        return hasOverAll(authority) || overTargets.getOrDefault(authority, Set.of()).contains(target);
    }

    public Set<TargetRef> targetsOf(Authority authority) {
        return overTargets.getOrDefault(authority, Set.of());
    }
}
