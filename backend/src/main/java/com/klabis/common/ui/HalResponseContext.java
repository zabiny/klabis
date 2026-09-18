package com.klabis.common.ui;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carries the domain object(s) behind a plain payload DTO returned by a controller method,
 * so {@link HalResponseBodyAdvice} can re-attach them to build {@code EntityModelWithDomain}
 * and run the existing {@code RepresentationModelProcessor} postprocessors.
 * <p>
 * Scoped to the current HTTP request via a request attribute (same mechanism as
 * {@link HalFormsSupport}'s inline-option context) — nothing survives past the request,
 * and nothing leaks between concurrent requests.
 */
public final class HalResponseContext {

    private static final String SINGLE_DOMAIN_ATTR = HalResponseContext.class.getName() + ".singleDomain";
    private static final String DOMAIN_LIST_ATTR = HalResponseContext.class.getName() + ".domainList";
    private static final String EMBEDDED_ATTR = HalResponseContext.class.getName() + ".embedded";
    private static final String CONTEXT_MAP_ATTR = HalResponseContext.class.getName() + ".contextMap";

    private HalResponseContext() {
    }

    /**
     * Registers the domain object backing a single-item response (e.g. {@code getMember}).
     */
    public static <D> void setDomain(D domain) {
        setAttribute(SINGLE_DOMAIN_ATTR, domain);
    }

    /**
     * Registers the domain objects backing a collection response, in the same order as the
     * DTO list returned by the controller — the advice pairs them 1:1 by index.
     */
    public static <D> void setDomainList(List<D> domainList) {
        setAttribute(DOMAIN_LIST_ATTR, domainList);
    }

    /**
     * Registers a collection to be rendered under {@code _embedded} next to a single-item payload,
     * for endpoints whose response carries a second, independently-shaped collection.
     * <p>
     * Declared here rather than in a postprocessor because the data usually needs a port the
     * controller already holds, and because the postprocessor for a given payload type may be shared
     * with a *list* endpoint that has no such collection (as {@code MembershipFeeGroupResponse} is).
     * The {@code _embedded} block itself is assembled by {@link HalResponseBodyAdvice} after the
     * postprocessors have run, so it lands alongside {@code _links} and {@code _templates}.
     * <p>
     * One collection per response: a second call replaces the first. Both current callers embed
     * exactly one, and supporting several would mean deciding how their relations compose inside the
     * {@code _embedded} object — worth adding only once an endpoint actually needs it.
     *
     * @param itemType fallback element type, so an empty collection still renders under the correct
     *                 relation instead of being dropped
     */
    public static void embed(Collection<?> collection, Class<?> itemType) {
        setAttribute(EMBEDDED_ATTR, new Embedded(collection, itemType));
    }

    static Embedded takeEmbedded() {
        return (Embedded) takeAttribute(EMBEDDED_ATTR);
    }

    record Embedded(Collection<?> collection, Class<?> itemType) {
    }

    @SuppressWarnings("unchecked")
    static <D> D takeDomain() {
        return (D) takeAttribute(SINGLE_DOMAIN_ATTR);
    }

    @SuppressWarnings("unchecked")
    static <D> List<D> takeDomainList() {
        return (List<D>) takeAttribute(DOMAIN_LIST_ATTR);
    }

    /**
     * Publishes an arbitrary value for a postprocessor to read back, keyed by its runtime class.
     * The type token is the key, so producer and consumer are linked by the compiler rather than
     * by a shared string. A second value of the same class replaces the first; values of different
     * classes coexist. No-op when no request attributes are bound.
     */
    public static void setContext(Object value) {
        Map<Class<?>, Object> map = contextMap(true);
        if (map != null) {
            map.put(value.getClass(), value);
        }
    }

    /**
     * Resolves a published value by assignability: returns a stored value whose runtime class is
     * assignable to {@code type}, so it can be read through an interface or supertype. Empty when
     * nothing matches or no request is bound.
     * <p>
     * <strong>Does not consume.</strong> Unlike {@link #takeDomain()} and friends, the value stays
     * in the map so it can be read again — {@code AccommodationListItemPostprocessor} reads the same
     * context once per row.
     *
     * @throws IllegalStateException if two stored values are both assignable to {@code type} —
     *                               picking one would depend on map iteration order
     */
    public static <T> Optional<T> findContext(Class<T> type) {
        Map<Class<?>, Object> map = contextMap(false);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(resolve(map, type));
    }

    /**
     * Same assignability lookup as {@link #findContext(Class)}, for a caller that declares the value
     * mandatory.
     * <p>
     * <strong>Does not consume.</strong> The value stays in the map so it can be read again —
     * {@code AccommodationListItemPostprocessor} reads the same context once per row.
     *
     * @throws IllegalStateException if nothing is stored for {@code type}, or if two stored values
     *                               are both assignable to it
     */
    public static <T> T getContext(Class<T> type) {
        Map<Class<?>, Object> map = contextMap(false);
        T value = map == null ? null : resolve(map, type);
        if (value == null) {
            throw new IllegalStateException("HalResponseContext has nothing stored for " + type.getName());
        }
        return value;
    }

    private static <T> T resolve(Map<Class<?>, Object> map, Class<T> type) {
        Class<?> match = null;
        for (Class<?> storedType : map.keySet()) {
            if (type.isAssignableFrom(storedType)) {
                if (match != null) {
                    throw new IllegalStateException(
                            "HalResponseContext has an ambiguous match for " + type.getName() + ": both "
                                    + match.getName() + " and " + storedType.getName());
                }
                match = storedType;
            }
        }
        return match == null ? null : type.cast(map.get(match));
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Object> contextMap(boolean createIfAbsent) {
        ServletRequestAttributes attributes = currentAttributes();
        if (attributes == null) {
            return null;
        }
        Map<Class<?>, Object> map =
                (Map<Class<?>, Object>) attributes.getAttribute(CONTEXT_MAP_ATTR, ServletRequestAttributes.SCOPE_REQUEST);
        if (map == null && createIfAbsent) {
            map = new HashMap<>();
            attributes.setAttribute(CONTEXT_MAP_ATTR, map, ServletRequestAttributes.SCOPE_REQUEST);
        }
        return map;
    }

    static void clear() {
        takeAttribute(SINGLE_DOMAIN_ATTR);
        takeAttribute(DOMAIN_LIST_ATTR);
        takeAttribute(EMBEDDED_ATTR);
        takeAttribute(CONTEXT_MAP_ATTR);
    }

    private static void setAttribute(String name, Object value) {
        ServletRequestAttributes attributes = currentAttributes();
        if (attributes != null) {
            attributes.setAttribute(name, value, ServletRequestAttributes.SCOPE_REQUEST);
        }
    }

    private static Object takeAttribute(String name) {
        ServletRequestAttributes attributes = currentAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(name, ServletRequestAttributes.SCOPE_REQUEST);
        attributes.removeAttribute(name, ServletRequestAttributes.SCOPE_REQUEST);
        return value;
    }

    private static ServletRequestAttributes currentAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }
}
