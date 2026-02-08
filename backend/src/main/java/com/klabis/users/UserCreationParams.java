package com.klabis.users;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builder for creating users with optional Personally Identifiable Information (PII).
 * <p>
 * Encapsulates user creation parameters including optional email for password setup coordination
 * across module boundaries. Uses builder pattern to make optional fields explicit.
 * <p>
 * Usage:
 * <pre>
 * UserCreationParams params = UserCreationParams.builder()
 *     .username("ZBM0501")
 *     .passwordHash(bcryptHash)
 *     .authorities(Set.of(Authority.MEMBERS_READ))
 *     .email("member@example.com")  // Optional PII
 *     .build();
 * </pre>
 * <p>
 * Design Rationale:
 * - Record ensures immutability and clean data structure
 * - Builder pattern makes optional email field explicit without overloading
 * - Forward-compatible for future optional fields
 *
 * @param username     the username (registration number), required
 * @param passwordHash BCrypt-hashed password, required
 * @param authorities  set of authorities to grant, required
 * @param email        optional email for password setup (may be null)
 */
@ValueObject
public record UserCreationParams(
        String username,
        String passwordHash,
        Set<Authority> authorities,
        String email) {

    /**
     * Compact constructor for validation.
     * Ensures all required fields are present.
     *
     * @throws IllegalArgumentException if any required field is null
     */
    public UserCreationParams {
        Objects.requireNonNull(username, "Username is required");
        Objects.requireNonNull(passwordHash, "Password hash is required");
        Objects.requireNonNull(authorities, "Authorities are required");
        // email may be null - it's optional
    }

    /**
     * Returns email as an Optional.
     * Provides safe API for optional PII handling.
     *
     * @return Optional containing email if present, empty otherwise
     */
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    /**
     * Creates a new builder for constructing UserCreationParams.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for UserCreationParams.
     * Provides fluent API for constructing user creation parameters with optional fields.
     */
    public static class Builder {

        private String username;
        private String passwordHash;
        private Set<Authority> authorities;
        private String email;

        /**
         * Sets the username (registration number).
         *
         * @param username the username, required
         * @return this builder for chaining
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the password hash.
         *
         * @param passwordHash BCrypt-hashed password, required
         * @return this builder for chaining
         */
        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        /**
         * Sets the authorities to grant.
         *
         * @param authorities set of authorities, required
         * @return this builder for chaining
         */
        public Builder authorities(Set<Authority> authorities) {
            this.authorities = authorities;
            return this;
        }

        /**
         * Sets the email (optional PII).
         *
         * @param email the email address, optional (may be null)
         * @return this builder for chaining
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Builds the UserCreationParams.
         * Validates that all required fields are set.
         *
         * @return new UserCreationParams instance
         * @throws IllegalArgumentException if any required field is missing
         */
        public UserCreationParams build() {
            return new UserCreationParams(username, passwordHash, authorities, email);
        }
    }
}
