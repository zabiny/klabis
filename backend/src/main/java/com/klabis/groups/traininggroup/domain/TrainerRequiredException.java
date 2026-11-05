package com.klabis.groups.traininggroup.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public class TrainerRequiredException extends InvalidDataException implements UserFacingError {

    private final @Nullable String groupName;

    public TrainerRequiredException() {
        this(null);
    }

    public TrainerRequiredException(@Nullable String groupName) {
        super("At least one trainer must remain in the group");
        this.groupName = groupName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return groupName == null ? Map.of() : Map.of("groupName", groupName);
    }

    @Override
    public String field() {
        return "trainers";
    }
}
