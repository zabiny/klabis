package com.klabis.events.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Value object holding up to three sequential registration deadlines for an event.
 * <p>
 * Invariants (enforced in constructor):
 * - deadline2 requires deadline1 to be present
 * - deadline3 requires deadline2 to be present
 * - deadlines must be non-decreasing: d1 <= d2 <= d3
 * <p>
 * The per-event-date constraint (each deadline <= event date) is validated by the
 * {@link Event} aggregate which owns the event date.
 */
@ValueObject
public record RegistrationDeadlines(
        Optional<LocalDate> deadline1,
        Optional<LocalDate> deadline2,
        Optional<LocalDate> deadline3
) {

    public RegistrationDeadlines {
        Assert.notNull(deadline1, "deadline1 must not be null");
        Assert.notNull(deadline2, "deadline2 must not be null");
        Assert.notNull(deadline3, "deadline3 must not be null");

        if (deadline2.isPresent() && deadline1.isEmpty()) {
            throw new IllegalArgumentException("deadline2 requires deadline1 to be present");
        }
        if (deadline3.isPresent() && deadline2.isEmpty()) {
            throw new IllegalArgumentException("deadline3 requires deadline2 to be present");
        }

        deadline1.ifPresent(d1 -> deadline2.ifPresent(d2 -> {
            if (d2.isBefore(d1)) {
                throw new IllegalArgumentException(
                        "deadline2 must not be before deadline1 (d1=%s, d2=%s)".formatted(d1, d2));
            }
        }));

        deadline2.ifPresent(d2 -> deadline3.ifPresent(d3 -> {
            if (d3.isBefore(d2)) {
                throw new IllegalArgumentException(
                        "deadline3 must not be before deadline2 (d2=%s, d3=%s)".formatted(d2, d3));
            }
        }));
    }

    /**
     * Constructs deadlines from nullable values (convenience factory for persistence layer).
     */
    public static RegistrationDeadlines of(LocalDate d1, LocalDate d2, LocalDate d3) {
        return new RegistrationDeadlines(
                Optional.ofNullable(d1),
                Optional.ofNullable(d2),
                Optional.ofNullable(d3)
        );
    }

    /**
     * Empty deadlines — no deadline configured.
     */
    public static RegistrationDeadlines none() {
        return new RegistrationDeadlines(Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Single deadline.
     */
    public static RegistrationDeadlines single(LocalDate d1) {
        return new RegistrationDeadlines(Optional.of(d1), Optional.empty(), Optional.empty());
    }

    /**
     * Returns the last deadline that is set (d3 if present, else d2, else d1).
     * Returns empty when no deadlines are configured.
     */
    public Optional<LocalDate> last() {
        if (deadline3.isPresent()) return deadline3;
        if (deadline2.isPresent()) return deadline2;
        return deadline1;
    }

    /**
     * Returns the nearest future deadline relative to {@code today}.
     * If all deadlines are in the past (or today), returns the last deadline.
     * Returns empty when no deadlines are configured.
     */
    public Optional<LocalDate> nextRelevant(LocalDate today) {
        if (deadline1.isEmpty()) {
            return Optional.empty();
        }
        if (deadline1.isPresent() && deadline1.get().isAfter(today)) {
            return deadline1;
        }
        if (deadline2.isPresent() && deadline2.get().isAfter(today)) {
            return deadline2;
        }
        if (deadline3.isPresent() && deadline3.get().isAfter(today)) {
            return deadline3;
        }
        return last();
    }

    /**
     * Returns true when registrations are still open as of {@code today}.
     * Registrations stay open through the entire last deadline day (inclusive) and only
     * close once that day has passed.
     * When no deadlines are configured, registrations are considered open (no deadline = unlimited).
     */
    public boolean registrationsOpen(LocalDate today) {
        return last().map(d -> !today.isAfter(d)).orElse(true);
    }

    /**
     * Returns true when no deadlines are configured.
     */
    public boolean isEmpty() {
        return deadline1.isEmpty();
    }
}
