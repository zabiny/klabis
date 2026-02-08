package com.klabis.members.management;

import com.klabis.common.exceptions.AuthorizationException;

/**
 * Exception thrown when a non-admin user attempts to update admin-only fields.
 * <p>
 * This exception is thrown when a member without MEMBERS:UPDATE authority
 * attempts to modify fields that are restricted to administrators.
 */
class AdminFieldAccessException extends AuthorizationException {

    private final String fieldName;

    public AdminFieldAccessException(String fieldName) {
        super(String.format(
                "Field '%s' is restricted to administrators. " +
                "Members without MEMBERS:UPDATE authority cannot modify this field.",
                fieldName
        ));
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
