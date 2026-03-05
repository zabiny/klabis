package com.klabis.members.application;

import com.klabis.common.ClubProperties;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MembersConfiguration {

    @Bean
    RegistrationNumberGenerator registrationNumberGenerator(
            ClubProperties clubProperties,
            MemberRepository memberRepository) {
        return new RegistrationNumberGenerator(clubProperties.getCode(), memberRepository);
    }
}
