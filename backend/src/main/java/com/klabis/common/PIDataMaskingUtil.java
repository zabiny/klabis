package com.klabis.common;

/**
 * Utility class for operations masking PI data.
 */
public final class PIDataMaskingUtil {

    private PIDataMaskingUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Masks an email address for security/logging purposes.
     *
     * <p>Examples:
     * <ul>
     *   <li>john.doe@example.com → j***@example.com</li>
     *   <li>a@example.com → a***@example.com</li>
     *   <li>@example.com → ***@***</li>
     * </ul>
     *
     * @param email the email address to mask
     * @return the masked email address
     */
    public static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***@***";
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return username.charAt(0) + "***" + domain;
    }
}
