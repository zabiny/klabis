package com.klabis.membershipfees.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class MembershipFeesConfiguration {

    @Bean
    Clock membershipFeesClock() {
        return Clock.systemDefaultZone();
    }
}
