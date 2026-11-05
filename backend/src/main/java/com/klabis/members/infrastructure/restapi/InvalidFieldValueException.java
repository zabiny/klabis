package com.klabis.members.infrastructure.restapi;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Value objects reject malformed input with {@link IllegalArgumentException}, which the ORIS import relies on.
 * Only the request mappers know which form field the value came from, so they translate it here.
 */
class InvalidFieldValueException extends InvalidDataException implements UserFacingError {

    enum Kind {
        BIRTH_NUMBER("invalid-birth-number", "birthNumber"),
        BANK_ACCOUNT_NUMBER("invalid-bank-account-number", "bankAccountNumber"),
        EMAIL("invalid-email-address", "email"),
        PHONE("invalid-phone-number", "phone");

        private final String errorType;
        private final String field;

        Kind(String errorType, String field) {
            this.errorType = errorType;
            this.field = field;
        }
    }

    private final Kind kind;

    InvalidFieldValueException(Kind kind, IllegalArgumentException cause) {
        super(cause.getMessage(), cause);
        this.kind = kind;
    }

    static <T> T parse(Kind kind, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldValueException(kind, e);
        }
    }

    @Override
    public String errorType() {
        return kind.errorType;
    }

    @Override
    public Set<String> errorTypes() {
        return Arrays.stream(Kind.values()).map(k -> k.errorType).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String field() {
        return kind.field;
    }
}
