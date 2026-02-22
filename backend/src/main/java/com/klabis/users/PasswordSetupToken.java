package com.klabis.users;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root representing a password setup token for user account activation.
 *
 * <p>Password setup tokens are sent via email to allow new users to set their own passwords.
 * They are designed to be:
 * <ul>
 *   <li>Cryptographically secure (random UUID tokens)</li>
 *   <li>Time-limited (expire after configured period, typically 4 hours)</li>
 *   <li>Single-use (marked as used after password is set)</li>
 *   <li>Securely stored (SHA-256 hashed, never plain text)</li>
 * </ul>
 *
 * <p>This is an aggregate root with the following invariants:
 * <ul>
 *   <li>Token must have a valid expiration time in the future</li>
 *   <li>Token can only be used once (after being marked as used, it cannot be reused)</li>
 *   <li>Token hash is immutable (once created, the hash never changes)</li>
 * </ul>
 *
 * <p>Factory method: {@link #generateFor(UserId, Duration)}
 */
@AggregateRoot
public class PasswordSetupToken extends KlabisAggregateRoot<PasswordSetupToken, UUID> {

    @Identity
    private final UUID id;
    private final UserId userId;
    private final TokenHash tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant usedAt;
    private String usedByIp;
    private boolean isNew = true;

    /**
     * Private constructor. Use factory method {@link #generateFor(UserId, Duration)} to create instances.
     *
     * @param id            unique token identifier
     * @param userId        user ID requiring password setup
     * @param tokenHash     SHA-256 hash of the random token
     * @param createdAt     token creation timestamp
     * @param expiresAt     token expiration timestamp
     * @param auditMetadata audit metadata
     */
    private PasswordSetupToken(UUID id, UserId userId, TokenHash tokenHash, Instant createdAt, Instant expiresAt, AuditMetadata auditMetadata) {
        this.id = Objects.requireNonNull(id, "Token ID is required");
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration time is required");

        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiration time must be after creation time");
        }

        updateAuditMetadata(auditMetadata);
    }

    /**
     * Factory method to generate a new password setup token for a user.
     *
     * @param userId           the user requiring password setup
     * @param validityPeriod how long until the token expires
     * @return new PasswordSetupToken with random UUID token
     * @throws IllegalArgumentException if user or validityPeriod is null
     */
    public static PasswordSetupToken generateFor(UserId userId, Duration validityPeriod) {
        Assert.notNull(userId, "User is required");
        Assert.notNull(validityPeriod, "Validity period is required");

        if (validityPeriod.isNegative() || validityPeriod.isZero()) {
            throw new IllegalArgumentException("Validity period must be positive");
        }

        UUID tokenId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiration = now.plus(validityPeriod);

        // Generate random token and hash it
        String plainToken = UUID.randomUUID().toString();
        TokenHash hash = TokenHash.hash(plainToken);

        PasswordSetupToken token = new PasswordSetupToken(tokenId, userId, hash, now, expiration, null);
        token.plainToken = plainToken; // Store temporarily for email sending

        return token;
    }

    /**
     * Factory method to reconstruct a token from persisted data (for repository use).
     *
     * <p>This is used by the repository when loading tokens from the database.
     * The plain text token will not be available (it's never persisted).
     *
     * @param id              token ID
     * @param userId          user ID
     * @param tokenHash       hash of the token
     * @param createdAt       creation timestamp
     * @param expiresAt       expiration timestamp
     * @param usedAt          usage timestamp (null if unused)
     * @param usedByIp        IP address of usage (null if unused)
     * @param auditMetadata   audit metadata with createdAt, createdBy, lastModifiedAt, lastModifiedBy, version
     * @return reconstructed PasswordSetupToken
     */
    public static PasswordSetupToken reconstruct(
            UUID id,
            UserId userId,
            TokenHash tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            String usedByIp,
            AuditMetadata auditMetadata) {

        PasswordSetupToken token = new PasswordSetupToken(id, userId, tokenHash, createdAt, expiresAt, auditMetadata);

        // Set used status if token was used
        if (usedAt != null && usedByIp != null) {
            token.usedAt = usedAt;
            token.usedByIp = usedByIp;
        }

        // Mark as persisted (loaded from database)
        token.isNew = false;

        // Note: plainToken remains null (not persisted)

        return token;
    }

    // Temporary storage of plain token for email sending (not persisted)
    private transient String plainToken;

    /**
     * Gets the plain text token (for email sending only).
     *
     * <p>This is only available immediately after token generation via {@link #generateFor(UserId, Duration)}.
     * After persistence and retrieval, this field will be null.
     *
     * @return plain text token, or null if not available
     */
    public String getPlainText() {
        return plainToken;
    }

    /**
     * Checks if this is a new (unsaved) PasswordSetupToken instance.
     * <p>
     * Used by Spring Data JDBC via Persistable.isNew() to determine INSERT vs UPDATE.
     *
     * @return true if this is a new instance, false if already persisted
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Marks this PasswordSetupToken as persisted (no longer new).
     * <p>
     * Called by the repository after save operation.
     */
    public void markAsPersisted() {
        this.isNew = false;
    }

    /**
     * Marks this token as used by setting the usage timestamp and IP address.
     *
     * @param ipAddress the IP address of the user who used this token
     * @throws IllegalStateException if token is already used or expired
     */
    public void markAsUsed(String ipAddress) {
        if (isUsed()) {
            throw new IllegalStateException("Token has already been used");
        }

        if (isExpired()) {
            throw new IllegalStateException("Token has expired");
        }

        Assert.hasText(ipAddress, "IP address is required");

        this.usedAt = Instant.now();
        this.usedByIp = ipAddress;
    }

    /**
     * Checks if this token has expired.
     *
     * @return true if the token is past its expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this token has been used.
     *
     * @return true if the token has been marked as used
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Checks if this token is valid (can be used for password setup).
     *
     * <p>A token is valid if it has not expired and has not been used yet.
     *
     * @return true if the token is still valid
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * Verifies that the given plain text token matches this token's hash.
     *
     * @param plainToken the plain text token to verify
     * @return true if the hash matches
     */
    public boolean verify(String plainToken) {
        if (plainToken == null) {
            return false;
        }
        return tokenHash.matches(plainToken);
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public TokenHash getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public String getUsedByIp() {
        return usedByIp;
    }

    public AuditMetadata getAuditMetadata() {
        return super.getAuditMetadata();
    }

    public Instant getLastModifiedAt() {
        return super.getLastModifiedAt();
    }

    public String getCreatedBy() {
        return super.getCreatedBy();
    }

    public String getLastModifiedBy() {
        return super.getLastModifiedBy();
    }

    public Long getVersion() {
        return super.getVersion();
    }

    public LocalDateTime getCreatedAtLocal() {
        return LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC);
    }

    public LocalDateTime getExpiresAtLocal() {
        return LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
    }

    public LocalDateTime getUsedAtLocal() {
        return usedAt != null ? LocalDateTime.ofInstant(usedAt, ZoneOffset.UTC) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordSetupToken that = (PasswordSetupToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PasswordSetupToken{" +
               "id=" + id +
               ", userId=" + userId +
               ", createdAt=" + createdAt +
               ", expiresAt=" + expiresAt +
               ", usedAt=" + usedAt +
               ", usedByIp='" + usedByIp + '\'' +
               '}';
    }
}
