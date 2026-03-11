package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.LocalDate;

@ValueObject
public record RefereeLicense(RefereeLevel level, LocalDate validityDate) {

    public RefereeLicense {
        Assert.notNull(level, "Referee license level is required");
        Assert.notNull(validityDate, "Referee license validity date is required");
    }

    public static RefereeLicense of(RefereeLevel level, LocalDate validityDate) {
        return new RefereeLicense(level, validityDate);
    }
}
