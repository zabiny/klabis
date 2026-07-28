package com.klabis.common.ui;

import org.springframework.hateoas.Link;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
    private static final String SELF_LINK_SUPPLIER_ATTR = HalResponseContext.class.getName() + ".selfLinkSupplier";

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

    @SuppressWarnings("unchecked")
    static <D> D takeDomain() {
        return (D) takeAttribute(SINGLE_DOMAIN_ATTR);
    }

    @SuppressWarnings("unchecked")
    static <D> List<D> takeDomainList() {
        return (List<D>) takeAttribute(DOMAIN_LIST_ATTR);
    }

    /**
     * Registers a supplier for the collection-level self link (e.g. one that re-encodes the
     * request's query/pagination parameters via {@code klabisLinkTo(methodOn(...))}), applied by
     * the advice once the {@code PagedModel} exists.
     * <p>
     * {@code klabisLinkTo} returns {@link Optional#empty()} when the caller isn't authorized for
     * the linked method — the supplier must propagate that as {@code Optional.empty()}, not
     * {@code null}, so the advice can leave the assembler-provided self link untouched instead of
     * overwriting it with a link built from a null-producing mapper.
     */
    public static void setSelfLinkSupplier(Supplier<Optional<Link>> selfLinkSupplier) {
        setAttribute(SELF_LINK_SUPPLIER_ATTR, selfLinkSupplier);
    }

    @SuppressWarnings("unchecked")
    static Supplier<Optional<Link>> takeSelfLinkSupplier() {
        return (Supplier<Optional<Link>>) takeAttribute(SELF_LINK_SUPPLIER_ATTR);
    }

    static void clear() {
        takeAttribute(SINGLE_DOMAIN_ATTR);
        takeAttribute(DOMAIN_LIST_ATTR);
        takeAttribute(SELF_LINK_SUPPLIER_ATTR);
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
