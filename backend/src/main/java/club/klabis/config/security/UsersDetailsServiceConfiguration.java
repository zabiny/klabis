package club.klabis.config.security;

import club.klabis.domain.users.Member;
import club.klabis.domain.users.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class UsersDetailsServiceConfiguration {

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
                return User.withUsername(member.getUserName()).password(member.getPassword()).build();
            }
        };
    }



}
