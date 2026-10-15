package com.klabis.members.application;

import org.jspecify.annotations.Nullable;

public record EffectiveContact(Value email, Value phone) {

    public static final EffectiveContact EMPTY = new EffectiveContact(Value.EMPTY, Value.EMPTY);

    public record Value(@Nullable String value, @Nullable String guardianName) {

        public static final Value EMPTY = new Value(null, null);

        public boolean fromGuardian() {
            return guardianName != null;
        }
    }
}
