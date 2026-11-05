package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class MemberDataRequiredException extends BusinessRuleViolationException implements UserFacingError {

    private final MissingDataItem item;

    public MemberDataRequiredException(MissingDataItem item) {
        super(messageOf(item));
        this.item = item;
    }

    private static String messageOf(MissingDataItem item) {
        return switch (item) {
            case EMAIL -> "At least one email address is required (member or guardian)";
            case PHONE -> "At least one phone number is required (member or guardian)";
            case ADDRESS -> "Address is required";
            case BIRTH_NUMBER -> "Birth number is required for Czech nationals";
            case GUARDIAN -> "Guardian is required for minors (under 18 years)";
        };
    }

    private static String typeOf(MissingDataItem item) {
        return switch (item) {
            case EMAIL -> "email-required";
            case PHONE -> "phone-required";
            case ADDRESS -> "address-required";
            case BIRTH_NUMBER -> "birth-number-required";
            case GUARDIAN -> "guardian-required";
        };
    }

    @Override
    public String errorType() {
        return typeOf(item);
    }

    @Override
    public Set<String> errorTypes() {
        return Arrays.stream(MissingDataItem.values()).map(MemberDataRequiredException::typeOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    // Guardians are set through their own resource, so a missing guardian has no form field of its own.
    @Override
    public @Nullable String field() {
        return switch (item) {
            case EMAIL -> "email";
            case PHONE -> "phone";
            case ADDRESS -> "address";
            case BIRTH_NUMBER -> "birthNumber";
            case GUARDIAN -> null;
        };
    }
}
