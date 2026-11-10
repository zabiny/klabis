package com.klabis.common.exceptions;

import java.util.List;

public final class CommonErrorTypes {

    private static final String PREFIX = "/errors/";

    public static final String VALIDATION_FAILED = uri("validation-failed");
    public static final String INVALID_REQUEST = uri("invalid-request");
    public static final String UNAUTHORIZED = uri("unauthorized");
    public static final String FORBIDDEN = uri("forbidden");
    public static final String ACCOUNT_INACTIVE = uri("account-inactive");
    public static final String NOT_FOUND = uri("not-found");
    public static final String CONFLICT = uri("conflict");
    public static final String CONCURRENT_UPDATE = uri("concurrent-update");
    public static final String UNEXPECTED = uri("unexpected");

    public static final List<String> ALL = List.of(
            VALIDATION_FAILED, INVALID_REQUEST, UNAUTHORIZED, FORBIDDEN, ACCOUNT_INACTIVE,
            NOT_FOUND, CONFLICT, CONCURRENT_UPDATE, UNEXPECTED);

    private CommonErrorTypes() {
    }

    public static String uri(String errorName) {
        return PREFIX + errorName;
    }
}
