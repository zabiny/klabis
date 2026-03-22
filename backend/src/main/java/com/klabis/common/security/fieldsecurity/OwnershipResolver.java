package com.klabis.common.security.fieldsecurity;

import org.springframework.security.core.Authentication;

/**
 * Resolves whether the current authenticated user is the owner of an object.
 * <p>
 * Implementations convert the owner identifier to a UUID and compare it against
 * the member ID carried in the authentication token.
 */
public interface OwnershipResolver {

    /**
     * Returns {@code true} if the authenticated user is the owner identified by
     * {@code ownerIdValue}.
     *
     * @param ownerIdValue the value of the field annotated with {@link OwnerId};
     *                     may be any type that can be converted to UUID via
     *                     {@code ConversionService}
     * @param authentication the current authentication
     * @return {@code true} if the user owns the object, {@code false} otherwise
     */
    boolean isOwner(Object ownerIdValue, Authentication authentication);
}
