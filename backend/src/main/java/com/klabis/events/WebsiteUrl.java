package com.klabis.events;

import org.jmolecules.ddd.annotation.ValueObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Value Object representing a website URL.
 * <p>
 * This value object encapsulates URL validation.
 * The URL value is trimmed of leading/trailing whitespace for consistency.
 * <p>
 * Business rules:
 * - URL must be valid according to java.net.URL
 * - URL must use http or https protocol
 * - URL cannot be null or blank
 */
@ValueObject
public record WebsiteUrl(String value) {

    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";

    /**
     * Creates a WebsiteUrl value object with validation.
     *
     * @param value website URL (required, must be valid http/https URL)
     * @throws IllegalArgumentException if validation fails
     */
    public WebsiteUrl {
        // Validate and trim
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Website URL is required");
        }
        String trimmedValue = value.trim();

        // Check for invalid characters (angle brackets are not allowed in URLs)
        if (trimmedValue.contains("<") || trimmedValue.contains(">")) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        // Validate URL format
        URL url;
        try {
            url = new URL(trimmedValue);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }

        // Must use http or https protocol
        String protocol = url.getProtocol().toLowerCase();
        if (!HTTP_PROTOCOL.equals(protocol) && !HTTPS_PROTOCOL.equals(protocol)) {
            throw new IllegalArgumentException("Website URL must use http or https protocol");
        }

        value = trimmedValue;
    }

    /**
     * Static factory method to create a WebsiteUrl from a string.
     *
     * @param value website URL string
     * @return WebsiteUrl value object
     * @throws IllegalArgumentException if validation fails
     */
    public static WebsiteUrl of(String value) {
        return new WebsiteUrl(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
