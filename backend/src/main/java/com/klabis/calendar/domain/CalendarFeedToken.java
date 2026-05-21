package com.klabis.calendar.domain;

import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Aggregate root for a per-user personal access token used to authenticate iCalendar feed requests.
 * <p>
 * Single token per user (Decision 2). Raw token is returned to caller on generate/regenerate
 * and never stored — only the hash and a non-secret lookup prefix are persisted.
 */
@AggregateRoot
public class CalendarFeedToken {

    private static final int TOKEN_BYTES = 32;
    public static final int LOOKUP_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Identity
    private final UserId userId;

    private String tokenHash;
    private String tokenLookup;
    private Instant lastSetAt;
    private boolean isNew;

    private CalendarFeedToken(UserId userId, String tokenHash, String tokenLookup, Instant lastSetAt, boolean isNew) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.tokenLookup = tokenLookup;
        this.lastSetAt = lastSetAt;
        this.isNew = isNew;
    }

    /**
     * Creates a new token for the given user. Returns the raw token (shown to user once).
     */
    public static Result generate(UserId userId, PasswordEncoder passwordEncoder) {
        Assert.notNull(userId, "userId must not be null");
        Assert.notNull(passwordEncoder, "passwordEncoder must not be null");

        String raw = generateRaw();
        String hash = passwordEncoder.encode(raw);
        String lookup = raw.substring(0, LOOKUP_LENGTH);
        Instant now = Instant.now();

        CalendarFeedToken token = new CalendarFeedToken(userId, hash, lookup, now, true);
        return new Result(token, raw);
    }

    /**
     * Reconstructs a token from the persistence layer. Bypasses validation.
     */
    public static CalendarFeedToken reconstruct(UserId userId, String tokenHash, String tokenLookup, Instant lastSetAt) {
        return new CalendarFeedToken(userId, tokenHash, tokenLookup, lastSetAt, false);
    }

    /**
     * Overwrites the token with a newly generated one. Invalidates the previous URL.
     * Returns the new raw token (shown to user once).
     */
    public String regenerate(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder must not be null");

        String raw = generateRaw();
        this.tokenHash = passwordEncoder.encode(raw);
        this.tokenLookup = raw.substring(0, LOOKUP_LENGTH);
        this.lastSetAt = Instant.now();
        return raw;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getTokenLookup() {
        return tokenLookup;
    }

    public Instant getLastSetAt() {
        return lastSetAt;
    }

    public boolean isNew() {
        return isNew;
    }

    private static String generateRaw() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Carries both the aggregate (for persistence) and the raw token (for the caller).
     */
    public record Result(CalendarFeedToken token, String rawToken) {}
}
