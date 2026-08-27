package com.klabis.common.ui;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;

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

    static void clear() {
        takeAttribute(SINGLE_DOMAIN_ATTR);
        takeAttribute(DOMAIN_LIST_ATTR);
        takeAttribute(EMBEDDED_ATTR);
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
