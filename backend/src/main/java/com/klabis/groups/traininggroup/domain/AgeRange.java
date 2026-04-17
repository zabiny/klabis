package com.klabis.groups.traininggroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import org.springframework.util.Assert;

public record AgeRange(int minAge, int maxAge) {

    public AgeRange {
        Assert.isTrue(minAge >= 0, "minAge must be non-negative");
        Assert.isTrue(maxAge >= minAge, "maxAge must be >= minAge");
    }

    public boolean includes(int age) {
        return age >= minAge && age <= maxAge;
    }

    public boolean overlaps(AgeRange other) {
        return this.minAge <= other.maxAge && other.minAge <= this.maxAge;
    }

    public static final class OverlappingAgeRangeException extends BusinessRuleViolationException {
        public OverlappingAgeRangeException(AgeRange existing) {
            super("Age range overlaps with existing training group range [%d-%d]".formatted(existing.minAge(), existing.maxAge()));
        }
    }
}
