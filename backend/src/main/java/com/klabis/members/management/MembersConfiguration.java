package com.klabis.members.management;

import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Members module.
 * <p>
 * Configures beans required for member registration and management.
 */
@Configuration
class MembersConfiguration {

    /**
     * Creates the RegistrationNumberGenerator bean.
     * <p>
     * The generator uses the club code from configuration and the Members repository
     * to generate unique registration numbers for new members.
     *
     * @param clubCode the club code (3 characters) for registration number generation
     * @param memberRepository  the members repository for counting existing members by birth year
     * @return configured RegistrationNumberGenerator bean
     */
    @Bean
    RegistrationNumberGenerator registrationNumberGenerator(
            @Value("${klabis.members.club-code:ZBM}") String clubCode,
            MemberRepository memberRepository) {
        return new RegistrationNumberGenerator(clubCode, memberRepository);
    }
}
