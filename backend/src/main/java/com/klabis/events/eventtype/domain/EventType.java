package com.klabis.events.eventtype.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.events.EventTypeId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.Optional;

@AggregateRoot
public class EventType extends KlabisAggregateRoot<EventType, EventTypeId> {

    private static final String COLOR_PATTERN = "^#[0-9a-fA-F]{6}$";

    @Identity
    private final EventTypeId id;
    private String name;
    private String color;
    private int sortOrder;

    @RecordBuilder
    public record CreateEventType(
            @NotBlank(message = "Event type name is required")
            @Size(max = 100, message = "Event type name must not exceed 100 characters")
            String name,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a valid hex code like #aabbcc")
            String color,
            Integer sortOrder
    ) {}

    @RecordBuilder
    public record UpdateEventType(
            @NotBlank(message = "Event type name is required")
            @Size(max = 100, message = "Event type name must not exceed 100 characters")
            String name,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a valid hex code like #aabbcc")
            String color,
            Integer sortOrder
    ) {}

    private EventType(EventTypeId id, String name, String color, int sortOrder, AuditMetadata auditMetadata) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        updateAuditMetadata(auditMetadata);
    }

    public static EventType create(CreateEventType command, int resolvedSortOrder) {
        validateName(command.name());
        validateColor(command.color());
        int order = command.sortOrder() != null ? command.sortOrder() : resolvedSortOrder;
        Assert.isTrue(order >= 0, "Sort order must be non-negative");
        return new EventType(EventTypeId.generate(), command.name(), command.color(), order, null);
    }

    public static EventType reconstruct(EventTypeId id, String name, String color, int sortOrder, AuditMetadata auditMetadata) {
        return new EventType(id, name, color, sortOrder, auditMetadata);
    }

    public void update(UpdateEventType command) {
        validateName(command.name());
        validateColor(command.color());
        if (command.sortOrder() != null) {
            Assert.isTrue(command.sortOrder() >= 0, "Sort order must be non-negative");
            this.sortOrder = command.sortOrder();
        }
        this.name = command.name();
        this.color = command.color();
    }

    private static void validateName(String name) {
        Assert.hasText(name, "Event type name is required");
        Assert.isTrue(name.length() <= 100, "Event type name must not exceed 100 characters");
    }

    private static void validateColor(String color) {
        if (color != null && !color.matches(COLOR_PATTERN)) {
            throw new IllegalArgumentException("Color must be a valid hex code like #aabbcc, got: " + color);
        }
    }

    @Override
    public EventTypeId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getColor() {
        return Optional.ofNullable(color);
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public String toString() {
        return "EventType{id=" + id + ", name='" + name + "', sortOrder=" + sortOrder + "}";
    }
}
