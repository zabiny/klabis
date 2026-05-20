package com.klabis.calendar.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Period;

@ConfigurationProperties(prefix = "klabis.ical.window")
class IcalWindowProperties {

    private Period past = Period.ofDays(30);
    private Period future = Period.ofMonths(12);

    public Period getPast() {
        return past;
    }

    public void setPast(Period past) {
        this.past = past;
    }

    public Period getFuture() {
        return future;
    }

    public void setFuture(Period future) {
        this.future = future;
    }
}
