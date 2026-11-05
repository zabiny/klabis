package com.klabis.groups.traininggroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;
import org.springframework.util.Assert;

public record AgeRange(int minAge, int maxAge) {

    public AgeRange {
        Assert.isTrue(minAge >= 0, "minAge must be non-negative");
        if (maxAge < minAge) {
            throw new InvalidAgeRangeException(minAge, maxAge);
        }
    }

    public boolean includes(int age) {
        return age >= minAge && age <= maxAge;
    }

    public boolean overlaps(AgeRange other) {
        return this.minAge <= other.maxAge && other.minAge <= this.maxAge;
    }

    public static final class OverlappingAgeRangeException extends BusinessRuleViolationException implements UserFacingError {

        private final AgeRange refused;

        public OverlappingAgeRangeException(AgeRange refused) {
            super("Age range [%d-%d] overlaps with an existing training group range".formatted(refused.minAge(), refused.maxAge()));
            this.refused = refused;
        }

        @Override
        public Map<String, Object> messageParameters() {
            return Map.of("minAge", refused.minAge(), "maxAge", refused.maxAge());
        }
    }
}
