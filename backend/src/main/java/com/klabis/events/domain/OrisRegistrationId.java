package com.klabis.events.domain;

/**
 * Identity of a registration on the ORIS side: {@code <orisEventId>:<clubUserId>}, the key under which ORIS lists
 * the entries of a club.
 */
public record OrisRegistrationId(int orisEventId, int clubUserId) {

    private static final char SEPARATOR = ':';

    public static OrisRegistrationId parse(String value) {
        int separator = value == null ? -1 : value.indexOf(SEPARATOR);
        if (separator < 0) {
            throw new IllegalArgumentException("Not an ORIS registration id: '" + value + "'");
        }
        try {
            return new OrisRegistrationId(
                    Integer.parseInt(value.substring(0, separator)),
                    Integer.parseInt(value.substring(separator + 1)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not an ORIS registration id: '" + value + "'", e);
        }
    }

    public String format() {
        return orisEventId + String.valueOf(SEPARATOR) + clubUserId;
    }
}
