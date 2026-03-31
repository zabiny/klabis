package com.klabis.common.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.Set;

class CollectionPropertyContext {

    private static final String REQUEST_ATTR = CollectionPropertyContext.class.getName() + ".collectionProperties";

    static void markCollectionProperty(String propertyName) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Set<String> names = (Set<String>) request.getAttribute(REQUEST_ATTR);
        if (names == null) {
            names = new HashSet<>();
            request.setAttribute(REQUEST_ATTR, names);
        }
        names.add(propertyName);
    }

    static boolean isCollectionProperty(String propertyName) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Set<String> names = (Set<String>) request.getAttribute(REQUEST_ATTR);
        return names != null && names.contains(propertyName);
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
