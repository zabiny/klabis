package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.LocalDate;

@ValueObject
public record TrainerLicense(TrainerLevel level, LocalDate validityDate) {

    public TrainerLicense {
        Assert.notNull(level, "Trainer license level is required");
        Assert.notNull(validityDate, "Trainer license validity date is required");
    }

    public static TrainerLicense of(TrainerLevel level, LocalDate validityDate) {
        return new TrainerLicense(level, validityDate);
    }
}
