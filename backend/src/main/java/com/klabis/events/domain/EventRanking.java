package com.klabis.events.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record EventRanking(int levelId, String shortName, String name) {

    public EventRanking {
        Assert.isTrue(levelId > 0, "levelId must be positive");
        Assert.hasText(shortName, "shortName must not be blank");
        Assert.hasText(name, "name must not be blank");
    }

    public static EventRanking of(int levelId, String shortName, String name) {
        return new EventRanking(levelId, shortName, name);
    }
}
