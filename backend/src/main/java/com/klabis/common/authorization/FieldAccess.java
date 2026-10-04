package com.klabis.common.authorization;

/**
 * What the current user may do with a request field: change it, only see it, or neither.
 */
public enum FieldAccess {
    NONE,
    READ,
    WRITE
}
