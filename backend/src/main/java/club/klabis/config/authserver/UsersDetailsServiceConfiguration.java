package club.klabis.config.authserver;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
class UsersDetailsServiceConfiguration {

    private final MemberService memberService;

    public UsersDetailsServiceConfiguration(MemberService memberService) {
        this.memberService = memberService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return memberService.findByUserName(username)
                        .map(this::fromMember)
                        .orElseThrow(() -> new UsernameNotFoundException("User with username %s not found".formatted(username)));
            }

            private UserDetails fromMember(Member member) {
                return User.withUsername(member.getRegistration().toRegistrationId()).password(member.getPassword()).build();
            }
        };
    }



}
