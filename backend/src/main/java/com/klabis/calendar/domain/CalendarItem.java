package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.common.domain.KlabisAggregateRoot;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;
import com.klabis.events.EventId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract aggregate root for calendar items.
 * <p>
 * Subtypes express the two distinct roles:
 * - {@link ManualCalendarItem} — created, updated and deleted by users
 * - {@link EventCalendarItem} — read-only, lifecycle tied to an Event
 * <p>
 * {@link #assertCanBeDeleted()} is abstract: each subtype declares whether deletion is allowed.
 */
@AggregateRoot
public abstract class CalendarItem extends KlabisAggregateRoot<CalendarItem, CalendarItemId> {

    // ========== Nested Command Records ==========

    @RecordBuilder
    public record CreateCalendarItem(
            @NotBlank(message = "Calendar item name is required")
            @Size(max = 200, message = "Calendar item name must not exceed 200 characters")
            String name,

            @Size(max = 1000, message = "Calendar item description must not exceed 1000 characters")
            String description,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate
    ) {}

    @RecordBuilder
    public record UpdateCalendarItem(
            @NotBlank(message = "Calendar item name is required")
            @Size(max = 200, message = "Calendar item name must not exceed 200 characters")
            String name,

            @Size(max = 1000, message = "Calendar item description must not exceed 1000 characters")
            String description,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate
    ) {}

    @RecordBuilder
    public record CreateCalendarItemForEvent(
            String name,
            String location,
            String organizer,
            String websiteUrl,
            LocalDate eventDate,
            EventId eventId
    ) {
        public String description() {
            return buildEventDescription(location, organizer, websiteUrl);
        }
    }

    @RecordBuilder
    public record SynchronizeFromEvent(
            String name,
            String location,
            String organizer,
            String websiteUrl,
            LocalDate eventDate
    ) {
        public String description() {
            return buildEventDescription(location, organizer, websiteUrl);
        }
    }

    static String buildEventDescription(String location, String organizer, String websiteUrl) {
        List<String> parts = new ArrayList<>();
        if (location != null && !location.isBlank()) parts.add(location);
        if (organizer != null && !organizer.isBlank()) parts.add(organizer);

        String base = parts.isEmpty() ? null : String.join(" - ", parts);

        if (websiteUrl != null && !websiteUrl.isBlank()) {
            return base != null ? base + "\n" + websiteUrl : websiteUrl;
        }
        return base;
    }

    @Identity
    private final CalendarItemId id;

    String name;
    String description;
    LocalDate startDate;
    LocalDate endDate;

    CalendarItem(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // ========== Shared Validation ==========

    static void validateName(String name) {
        Assert.hasText(name, "Calendar item name is required");
    }

    static void validateStartDate(LocalDate startDate) {
        Assert.notNull(startDate, "Start date is required");
    }

    static void validateEndDate(LocalDate endDate) {
        Assert.notNull(endDate, "End date is required");
    }

    static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        Assert.isTrue(!endDate.isBefore(startDate), "End date must be on or after start date");
    }

    // ========== Abstract Domain Method ==========

    public abstract void assertCanBeDeleted();

    // ========== Getters ==========

    @Override
    public CalendarItemId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
