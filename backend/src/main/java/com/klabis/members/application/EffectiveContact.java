package com.klabis.members.application;

import org.jspecify.annotations.Nullable;

public record EffectiveContact(Value email, Value phone) {

    public record Value(@Nullable String value, @Nullable String guardianName) {

        static final Value EMPTY = new Value(null, null);

        public boolean fromGuardian() {
            return guardianName != null;
        }
    }
}
